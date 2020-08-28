/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.dfa.cfg

import org.jetbrains.kotlin.contracts.description.EventOccurrencesRange
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirSymbolOwner
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.references.FirControlFlowGraphReference
import org.jetbrains.kotlin.fir.render
import org.jetbrains.kotlin.fir.resolve.dfa.*
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.resultType
import org.jetbrains.kotlin.fir.symbols.AbstractFirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.fir.types.isNothing
import org.jetbrains.kotlin.fir.visitors.FirDefaultVisitorVoid
import org.jetbrains.kotlin.utils.addToStdlib.runIf
import kotlin.random.Random

@RequiresOptIn
private annotation class CfgBuilderInternals

class ControlFlowGraphBuilder {
    @CfgBuilderInternals
    private val graphs: Stack<ControlFlowGraph> = stackOf(ControlFlowGraph(null, "<TOP_LEVEL_GRAPH>", ControlFlowGraph.Kind.TopLevel))

    @get:OptIn(CfgBuilderInternals::class)
    val currentGraph: ControlFlowGraph
        get() = graphs.top()

    private val lastNodes: Stack<CFGNode<*>> = stackOf()
    val lastNode: CFGNode<*>
        get() = lastNodes.top()

    var levelCounter: Int = 0
        private set

    private val modes: Stack<Mode> = stackOf(Mode.TopLevel)
    private val mode: Mode get() = modes.top()

    /*
     * TODO: it's temporary hack for anonymous functions resolved twice in delegate expressions
     * Example: val x: Boolean by lazy { true }
     *
     * Note that this hack breaks passing data flow from inplace lambdas inside lambdas of delegates:
     * val x: Boolean by lazy {
     *     val b: Any = ...
     *     run {
     *         require(b is Boolean)
     *     }
     *     b // there will be no smartcast, but it should be
     * }
     */
    private val shouldPassFlowFromInplaceLambda: Stack<Boolean> = stackOf(true)

    private enum class Mode {
        Function, TopLevel, Body, ClassInitializer, PropertyInitializer
    }

    // ----------------------------------- Node caches -----------------------------------

    private val exitTargetsForReturn: SymbolBasedNodeStorage<FirFunction<*>, FunctionExitNode> = SymbolBasedNodeStorage()
    private val exitTargetsForTry: Stack<CFGNode<*>> = stackOf()
    private val exitsOfAnonymousFunctions: MutableMap<FirFunctionSymbol<*>, FunctionExitNode> = mutableMapOf()
    private val enterToLocalClassesMembers: MutableMap<AbstractFirBasedSymbol<*>, CFGNode<*>?> = mutableMapOf()

    private val postponedLambdas: MutableSet<FirFunctionSymbol<*>> = mutableSetOf()
    private val entersToPostponedAnonymousFunctions: MutableMap<FirFunctionSymbol<*>, PostponedLambdaEnterNode> = mutableMapOf()
    private val exitsFromPostponedAnonymousFunctions: MutableMap<FirFunctionSymbol<*>, PostponedLambdaExitNode> = mutableMapOf()
    private val parentGraphForAnonymousFunctions: MutableMap<FirFunctionSymbol<*>, ControlFlowGraph> = mutableMapOf()

    private val loopEnterNodes: NodeStorage<FirElement, CFGNode<FirElement>> = NodeStorage()
    private val loopExitNodes: NodeStorage<FirLoop, LoopExitNode> = NodeStorage()

    private val exitsFromCompletedPostponedAnonymousFunctions: MutableList<PostponedLambdaExitNode> = mutableListOf()

    private val whenExitNodes: NodeStorage<FirWhenExpression, WhenExitNode> = NodeStorage()

    private val binaryAndExitNodes: Stack<BinaryAndExitNode> = stackOf()
    private val binaryOrExitNodes: Stack<BinaryOrExitNode> = stackOf()

    private val tryExitNodes: NodeStorage<FirTryExpression, TryExpressionExitNode> = NodeStorage()
    private val catchNodeStorages: Stack<NodeStorage<FirCatch, CatchClauseEnterNode>> = stackOf()
    private val catchNodeStorage: NodeStorage<FirCatch, CatchClauseEnterNode> get() = catchNodeStorages.top()
    private val finallyEnterNodes: Stack<FinallyBlockEnterNode> = stackOf()

    private val initBlockExitNodes: Stack<InitBlockExitNode> = stackOf()

    private val exitSafeCallNodes: Stack<ExitSafeCallNode> = stackOf()
    private val exitElvisExpressionNodes: Stack<ElvisExitNode> = stackOf()

    /*
     * ignoredFunctionCalls is needed for resolve of += operator:
     *   we have two different calls for resolve, but we left only one of them,
     *   so we twice call `enterCall` and twice increase `levelCounter`, but
     *   `exitFunctionCall` we call only once.
     *
     * So workflow looks like that:
     *   Calls:
     *     - a.plus(b) // (1)
     *     - a.plusAssign(b) // (2)
     *
     * enterCall(a.plus(b)), increase counter
     * exitIgnoredCall(a.plus(b)) // decrease counter
     * enterCall(a.plusAssign(b)) // increase counter
     * exitIgnoredCall(a.plusAssign(b)) // decrease counter
     * exitFunctionCall(a.plus(b) | a.plusAssign(b)) // don't touch counter
     */
    private val ignoredFunctionCalls: MutableSet<FirFunctionCall> = mutableSetOf()

    // ----------------------------------- API for node builders -----------------------------------

    private var idCounter: Int = Random.nextInt()
    fun createId(): Int = idCounter++

    // ----------------------------------- Public API -----------------------------------

    fun returnExpressionsOfAnonymousFunction(function: FirAnonymousFunction): Collection<FirStatement> {
        fun FirElement.extractArgument(): FirElement = when {
            this is FirReturnExpression && target.labeledElement.symbol == function.symbol -> result.extractArgument()
            else -> this
        }

        fun CFGNode<*>.extractArgument(): FirElement? = when (this) {
            is FunctionEnterNode, is TryMainBlockEnterNode, is CatchClauseEnterNode -> null
            is ExitSafeCallNode -> lastPreviousNode.extractArgument()
            is StubNode, is BlockExitNode -> firstPreviousNode.extractArgument()
            else -> fir.extractArgument()
        }

        val exitNode = function.controlFlowGraphReference?.controlFlowGraph?.exitNode
            ?: exitsOfAnonymousFunctions.getValue(function.symbol)
        return exitNode.previousNodes.mapNotNullTo(mutableSetOf()) {
            it.extractArgument() as FirStatement?
        }
    }

    @OptIn(CfgBuilderInternals::class)
    fun isTopLevel(): Boolean = graphs.size == 1

    // ----------------------------------- Utils -----------------------------------

    @OptIn(CfgBuilderInternals::class)
    private fun pushGraph(graph: ControlFlowGraph, mode: Mode) {
        graphs.push(graph)
        modes.push(mode)
        levelCounter++
    }

    @OptIn(CfgBuilderInternals::class)
    private fun popGraph(): ControlFlowGraph {
        levelCounter--
        modes.pop()
        return graphs.pop().also { it.complete() }
    }

    // ----------------------------------- Regular function -----------------------------------

    fun enterFunction(function: FirFunction<*>): Triple<FunctionEnterNode, LocalFunctionDeclarationNode?, CFGNode<*>?> {
        require(function !is FirAnonymousFunction)
        val name = when (function) {
            is FirSimpleFunction -> function.name.asString()
            is FirPropertyAccessor -> if (function.isGetter) "<getter>" else "<setter>"
            is FirConstructor -> "<init>"
            else -> throw IllegalArgumentException("Unknown function: ${function.render()}")
        }
        val graph = ControlFlowGraph(function, name, ControlFlowGraph.Kind.Function)
        // function is local
        val localFunctionNode = runIf(mode == Mode.Body) {
            assert(currentGraph.kind.withBody)
            currentGraph.addSubGraph(graph)

            createLocalFunctionDeclarationNode(function).also {
                addNewSimpleNode(it)
            }
        }

        pushGraph(
            graph = graph,
            mode = Mode.Body
        )

        val previousNode = enterToLocalClassesMembers[function.symbol]
            ?: (function as? FirSimpleFunction)?.takeIf { it.isLocal }?.let { lastNode }

        val enterNode = createFunctionEnterNode(function).also {
            lastNodes.push(it)
        }

        if (previousNode != null) {
            addEdge(previousNode, enterNode, preferredKind = EdgeKind.DfgForward)
        }

        createFunctionExitNode(function).also {
            exitTargetsForReturn.push(it)
            exitTargetsForTry.push(it)
        }

        return Triple(enterNode, localFunctionNode, previousNode)
    }

    fun exitFunction(function: FirFunction<*>): Pair<FunctionExitNode, ControlFlowGraph> {
        require(function !is FirAnonymousFunction)
        val exitNode = exitTargetsForReturn.pop()
        popAndAddEdge(exitNode)
        val graph = popGraph()
        assert(exitNode == graph.exitNode)
        exitTargetsForTry.pop().also {
            assert(it == graph.exitNode)
        }
        graph.exitNode.updateDeadStatus()
        return graph.exitNode as FunctionExitNode to graph
    }

    // ----------------------------------- Anonymous function -----------------------------------

    fun visitPostponedAnonymousFunction(anonymousFunction: FirAnonymousFunction): Pair<PostponedLambdaEnterNode, PostponedLambdaExitNode> {
        val enterNode = createPostponedLambdaEnterNode(anonymousFunction)
        val exitNode = createPostponedLambdaExitNode(anonymousFunction)
        val symbol = anonymousFunction.symbol
        postponedLambdas += symbol
        entersToPostponedAnonymousFunctions[symbol] = enterNode
        exitsFromPostponedAnonymousFunctions[symbol] = exitNode
        parentGraphForAnonymousFunctions[symbol] = currentGraph
        popAndAddEdge(enterNode, preferredKind = EdgeKind.Forward)
        addEdge(enterNode, exitNode, preferredKind = EdgeKind.DfgForward)
        lastNodes.push(exitNode)
        return enterNode to exitNode
    }

    fun enterAnonymousFunction(anonymousFunction: FirAnonymousFunction): Pair<PostponedLambdaEnterNode?, FunctionEnterNode> {
        val invocationKind = anonymousFunction.invocationKind

        var previousNodeIsNew = false
        val symbol = anonymousFunction.symbol
        val previousNode = entersToPostponedAnonymousFunctions[symbol]
            ?: createPostponedLambdaEnterNode(anonymousFunction).also {
                addNewSimpleNode(it)
                entersToPostponedAnonymousFunctions[symbol] = it
                previousNodeIsNew = true
            }

        if (previousNodeIsNew) {
            assert(symbol !in exitsFromPostponedAnonymousFunctions)
            val exitFromLambda = createPostponedLambdaExitNode(anonymousFunction).also {
                exitsFromPostponedAnonymousFunctions[symbol] = it
            }
            addEdge(previousNode, exitFromLambda)
        }

        pushGraph(ControlFlowGraph(anonymousFunction, "<anonymous>", ControlFlowGraph.Kind.AnonymousFunction), Mode.Function)

        val enterNode = createFunctionEnterNode(anonymousFunction).also {
            if (previousNodeIsNew) {
                addNewSimpleNode(it)
            } else {
                addEdge(previousNode, it)
                lastNodes.push(it)
            }
        }
        val exitNode = createFunctionExitNode(anonymousFunction).also {
            exitsOfAnonymousFunctions[symbol] = it
            exitTargetsForReturn.push(it)
            exitTargetsForTry.push(it)
        }

        if (invocationKind.hasTowardEdge) {
            addEdge(enterNode, exitNode)
        }
        if (invocationKind.hasBackEdge) {
            addBackEdge(exitNode, enterNode)
        }

        return if (previousNodeIsNew) {
            previousNode to enterNode
        } else {
            null to enterNode
        }
    }

    private val EventOccurrencesRange?.hasTowardEdge: Boolean
        get() = when (this) {
            EventOccurrencesRange.AT_MOST_ONCE, EventOccurrencesRange.UNKNOWN -> true
            else -> false
        }

    private val EventOccurrencesRange?.hasBackEdge: Boolean
        get() = when (this) {
            EventOccurrencesRange.AT_LEAST_ONCE, EventOccurrencesRange.UNKNOWN -> true
            else -> false
        }

    fun exitAnonymousFunction(anonymousFunction: FirAnonymousFunction): Triple<FunctionExitNode, PostponedLambdaExitNode?, ControlFlowGraph> {


        val symbol = anonymousFunction.symbol
        val exitNode = exitsOfAnonymousFunctions.remove(symbol)!!.also {
            require(it == exitTargetsForReturn.pop())
            require(it == exitTargetsForTry.pop())
        }
        popAndAddEdge(exitNode)
        exitNode.updateDeadStatus()

        val graph = popGraph().also { graph ->
            assert(graph.declaration == anonymousFunction)
            assert(graph.exitNode == exitNode)
            exitsFromCompletedPostponedAnonymousFunctions.removeAll { it.owner == graph }
        }

        val postponedEnterNode = entersToPostponedAnonymousFunctions.remove(symbol)!!
        val postponedExitNode = exitsFromPostponedAnonymousFunctions.remove(symbol)!!

        val lambdaIsPostponedFromCall = postponedLambdas.remove(symbol)
        if (!lambdaIsPostponedFromCall) {
            lastNodes.push(postponedExitNode)
        }

        val invocationKind = anonymousFunction.invocationKind
        if (invocationKind != null) {
            addEdge(exitNode, postponedExitNode, preferredKind = EdgeKind.CfgForward)
        } else {
            val kind = if (postponedExitNode.isDead) EdgeKind.DeadForward else EdgeKind.CfgForward
            CFGNode.addJustKindEdge(postponedEnterNode, postponedExitNode, kind, propagateDeadness = true)
        }

        if (invocationKind == EventOccurrencesRange.EXACTLY_ONCE && shouldPassFlowFromInplaceLambda.top()) {
            exitsFromCompletedPostponedAnonymousFunctions += postponedExitNode
        }

        val containingGraph = parentGraphForAnonymousFunctions.remove(symbol) ?: currentGraph
        containingGraph.addSubGraph(graph)
        return if (lambdaIsPostponedFromCall) {
            Triple(exitNode, null, graph)
        } else {
            Triple(exitNode, postponedExitNode, graph)
        }
    }

    // ----------------------------------- Classes -----------------------------------

    fun enterClass() {
        pushGraph(
            ControlFlowGraph(null, "STUB_CLASS_GRAPH", ControlFlowGraph.Kind.Stub),
            mode = Mode.ClassInitializer
        )
    }

    fun exitClass() {
        popGraph()
    }

    fun exitClass(klass: FirClass<*>): ControlFlowGraph {
        exitClass()
        val name = when (klass) {
            is FirAnonymousObject -> "<anonymous object>"
            is FirRegularClass -> klass.name.asString()
            else -> throw IllegalArgumentException("Unknown class kind: ${klass::class}")
        }

        val classGraph = ControlFlowGraph(klass, name, ControlFlowGraph.Kind.ClassInitializer)
        pushGraph(classGraph, Mode.ClassInitializer)
        val exitNode = createClassExitNode(klass)
        var node: CFGNode<*> = createClassEnterNode(klass)
        var prevInitPartNode: CFGNode<*>? = null
        for (declaration in klass.declarations) {
            val graph = when (declaration) {
                is FirProperty -> declaration.controlFlowGraphReference?.controlFlowGraph
                is FirAnonymousInitializer -> declaration.controlFlowGraphReference?.controlFlowGraph
                else -> null
            } ?: continue

            createPartOfClassInitializationNode(declaration as FirControlFlowGraphOwner).also {
                addEdge(node, it, preferredKind = EdgeKind.CfgForward)
                addEdge(it, graph.enterNode, preferredKind = EdgeKind.CfgForward)
                node = graph.exitNode

                if (prevInitPartNode != null) addEdge(prevInitPartNode!!, it, preferredKind = EdgeKind.DeadForward)
                it.updateDeadStatus()
                prevInitPartNode = it
            }
        }
        addEdge(node, exitNode, preferredKind = EdgeKind.CfgForward)
        if (prevInitPartNode != null) addEdge(prevInitPartNode!!, exitNode, preferredKind = EdgeKind.DeadForward)
        exitNode.updateDeadStatus()
        return popGraph()
    }

    fun prepareForLocalClassMembers(members: Collection<FirSymbolOwner<*>>) {
        members.forEachMember {
            enterToLocalClassesMembers[it.symbol] = lastNodes.topOrNull()
        }
    }

    fun cleanAfterForLocalClassMembers(members: Collection<FirSymbolOwner<*>>) {
        members.forEachMember {
            enterToLocalClassesMembers.remove(it.symbol)
        }
    }

    fun exitLocalClass(klass: FirRegularClass): Pair<LocalClassExitNode, ControlFlowGraph> {
        val graph = exitClass(klass).also {
            currentGraph.addSubGraph(it)
        }
        val node = createLocalClassExitNode(klass).also {
            addNewSimpleNodeIfPossible(it)
        }
        visitLocalClassFunctions(klass, node)
        addEdge(node, graph.enterNode, preferredKind = EdgeKind.CfgForward)
        return node to graph
    }

    fun exitAnonymousObject(anonymousObject: FirAnonymousObject): Pair<AnonymousObjectExitNode, ControlFlowGraph> {
        val graph = exitClass(anonymousObject).also {
            currentGraph.addSubGraph(it)
        }
        val node = createAnonymousObjectExitNode(anonymousObject).also {
            // TODO: looks like there was some problem with enum initializers
            if (lastNodes.isNotEmpty) {
                addNewSimpleNode(it)
            }
        }
        visitLocalClassFunctions(anonymousObject, node)
        addEdge(node, graph.enterNode, preferredKind = EdgeKind.CfgForward)
        return node to graph
    }

    fun visitLocalClassFunctions(klass: FirClass<*>, node: CFGNodeWithCfgOwner<*>) {
        klass.declarations.filterIsInstance<FirFunction<*>>().forEach { function ->
            val functionGraph = function.controlFlowGraphReference?.controlFlowGraph
            if (functionGraph != null && functionGraph.owner == null) {
                addEdge(node, functionGraph.enterNode, preferredKind = EdgeKind.CfgForward)
                node.addSubGraph(functionGraph)
            }
        }
    }

    // ----------------------------------- Value parameters (and it's defaults) -----------------------------------

    fun enterValueParameter(valueParameter: FirValueParameter): EnterDefaultArgumentsNode? {
        if (valueParameter.defaultValue == null) return null
        val graph = ControlFlowGraph(valueParameter, "default value of ${valueParameter.name}", ControlFlowGraph.Kind.DefaultArgument)
        currentGraph.addSubGraph(graph)
        pushGraph(graph, Mode.Body)

        createExitDefaultArgumentsNode(valueParameter).also {
            exitTargetsForTry.push(it)
        }

        return createEnterDefaultArgumentsNode(valueParameter).also {
            addEdge(lastNode, it)
            lastNodes.push(it)
        }
    }

    fun exitValueParameter(valueParameter: FirValueParameter): Pair<ExitDefaultArgumentsNode, ControlFlowGraph>? {
        if (valueParameter.defaultValue == null) return null
        val exitNode = exitTargetsForTry.pop() as ExitDefaultArgumentsNode
        popAndAddEdge(exitNode)
        val graph = popGraph()
        require(exitNode == graph.exitNode)
        return exitNode to graph
    }

    // ----------------------------------- Block -----------------------------------

    fun enterBlock(block: FirBlock): BlockEnterNode {
        return createBlockEnterNode(block).also {
            addNewSimpleNode(it)
        }
    }

    fun exitBlock(block: FirBlock): CFGNode<*> {
        return createBlockExitNode(block).also {
            addNewSimpleNode(it)
        }
    }

    // ----------------------------------- Property -----------------------------------

    fun enterProperty(property: FirProperty): PropertyInitializerEnterNode? {
        if (property.initializer == null && property.delegate == null) return null

        val graph = ControlFlowGraph(property, "val ${property.name}", ControlFlowGraph.Kind.PropertyInitializer)
        pushGraph(graph, Mode.PropertyInitializer)

        val enterNode = createPropertyInitializerEnterNode(property)
        val exitNode = createPropertyInitializerExitNode(property)
        exitTargetsForTry.push(exitNode)

        enterToLocalClassesMembers[property.symbol]?.let {
            addEdge(it, enterNode, preferredKind = EdgeKind.DfgForward)
        }

        lastNodes.push(enterNode)
        return enterNode
    }

    fun exitProperty(property: FirProperty): Pair<PropertyInitializerExitNode, ControlFlowGraph>? {
        if (property.initializer == null && property.delegate == null) return null
        val exitNode = exitTargetsForTry.pop() as PropertyInitializerExitNode
        popAndAddEdge(exitNode)
        val graph = popGraph()
        assert(exitNode == graph.exitNode)
        return exitNode to graph
    }

    // ----------------------------------- Delegate -----------------------------------

    fun enterDelegateExpression() {
        shouldPassFlowFromInplaceLambda.push(false)
    }

    fun exitDelegateExpression() {
        shouldPassFlowFromInplaceLambda.pop()
    }

    // ----------------------------------- Operator call -----------------------------------

    fun exitTypeOperatorCall(typeOperatorCall: FirTypeOperatorCall): TypeOperatorCallNode {
        return createTypeOperatorCallNode(typeOperatorCall).also { addNewSimpleNode(it) }
    }

    fun exitComparisonExpression(comparisonExpression: FirComparisonExpression): ComparisonExpressionNode {
        return createComparisonExpressionNode(comparisonExpression).also { addNewSimpleNode(it) }
    }

    fun exitEqualityOperatorCall(equalityOperatorCall: FirEqualityOperatorCall): EqualityOperatorCallNode {
        return createEqualityOperatorCallNode(equalityOperatorCall).also { addNewSimpleNode(it) }
    }

    // ----------------------------------- Jump -----------------------------------

    fun exitJump(jump: FirJump<*>): JumpNode {
        val node = createJumpNode(jump)
        val nextNode = when (jump) {
            is FirReturnExpression -> exitTargetsForReturn[jump.target.labeledElement.symbol]
            is FirContinueExpression -> loopEnterNodes[jump.target.labeledElement]
            is FirBreakExpression -> loopExitNodes[jump.target.labeledElement]
            else -> throw IllegalArgumentException("Unknown jump type: ${jump.render()}")
        }
        addNodeWithJump(node, nextNode, isBack = jump is FirContinueExpression)
        return node
    }

    // ----------------------------------- When -----------------------------------

    fun enterWhenExpression(whenExpression: FirWhenExpression): WhenEnterNode {
        val node = createWhenEnterNode(whenExpression)
        addNewSimpleNode(node)
        whenExitNodes.push(createWhenExitNode(whenExpression))
        levelCounter++
        return node
    }

    fun enterWhenBranchCondition(whenBranch: FirWhenBranch): WhenBranchConditionEnterNode {
        return createWhenBranchConditionEnterNode(whenBranch).also { addNewSimpleNode(it) }.also { levelCounter++ }
    }

    fun exitWhenBranchCondition(whenBranch: FirWhenBranch): Pair<WhenBranchConditionExitNode, WhenBranchResultEnterNode> {
        levelCounter--
        val conditionExitNode = createWhenBranchConditionExitNode(whenBranch).also {
            addNewSimpleNode(it)
        }.also { levelCounter++ }
        val branchEnterNode = createWhenBranchResultEnterNode(whenBranch).also {
            lastNodes.push(it)
            addEdge(conditionExitNode, it)
        }
        return conditionExitNode to branchEnterNode
    }

    fun exitWhenBranchResult(whenBranch: FirWhenBranch): WhenBranchResultExitNode {
        levelCounter--
        val node = createWhenBranchResultExitNode(whenBranch)
        popAndAddEdge(node)
        val whenExitNode = whenExitNodes.top()
        addEdge(node, whenExitNode, propagateDeadness = false)
        return node
    }

    fun exitWhenExpression(whenExpression: FirWhenExpression): Pair<WhenExitNode, WhenSyntheticElseBranchNode?> {
        val whenExitNode = whenExitNodes.pop()
        // exit from last condition node still on stack
        // we should remove it
        val lastWhenConditionExit = lastNodes.pop()
        val syntheticElseBranchNode = if (!whenExpression.isExhaustive) {
            createWhenSyntheticElseBranchNode(whenExpression).apply {
                addEdge(lastWhenConditionExit, this)
                addEdge(this, whenExitNode)
            }
        } else null
        whenExitNode.updateDeadStatus()
        lastNodes.push(whenExitNode)
        dropPostponedLambdasForNonDeterministicCalls()
        levelCounter--
        return whenExitNode to syntheticElseBranchNode
    }

    // ----------------------------------- While Loop -----------------------------------

    fun enterWhileLoop(loop: FirLoop): Pair<LoopEnterNode, LoopConditionEnterNode> {
        val loopEnterNode = createLoopEnterNode(loop).also {
            addNewSimpleNode(it)
            loopEnterNodes.push(it)
        }
        loopExitNodes.push(createLoopExitNode(loop))
        levelCounter++
        val conditionEnterNode = createLoopConditionEnterNode(loop.condition).also {
            addNewSimpleNode(it)
            // put conditional node twice so we can refer it after exit from loop block
            lastNodes.push(it)
        }
        levelCounter++
        return loopEnterNode to conditionEnterNode
    }

    fun exitWhileLoopCondition(loop: FirLoop): Pair<LoopConditionExitNode, LoopBlockEnterNode> {
        levelCounter--
        val conditionExitNode = createLoopConditionExitNode(loop.condition)
        addNewSimpleNode(conditionExitNode)
        val conditionConstBooleanValue = conditionExitNode.booleanConstValue
        addEdge(conditionExitNode, loopExitNodes.top(), propagateDeadness = false, isDead = conditionConstBooleanValue == true)
        val loopBlockEnterNode = createLoopBlockEnterNode(loop)
        addNewSimpleNode(loopBlockEnterNode, conditionConstBooleanValue == false)
        levelCounter++
        return conditionExitNode to loopBlockEnterNode
    }

    fun exitWhileLoop(loop: FirLoop): Pair<LoopBlockExitNode, LoopExitNode> {
        loopEnterNodes.pop()
        levelCounter--
        val loopBlockExitNode = createLoopBlockExitNode(loop)
        popAndAddEdge(loopBlockExitNode)
        if (lastNodes.isNotEmpty) {
            val conditionEnterNode = lastNodes.pop()
            require(conditionEnterNode is LoopConditionEnterNode) { loop.render() }
            addBackEdge(loopBlockExitNode, conditionEnterNode)
        }
        val loopExitNode = loopExitNodes.pop()
        loopExitNode.updateDeadStatus()
        lastNodes.push(loopExitNode)
        levelCounter--
        return loopBlockExitNode to loopExitNode
    }

    // ----------------------------------- Do while Loop -----------------------------------

    fun enterDoWhileLoop(loop: FirLoop): Pair<LoopEnterNode, LoopBlockEnterNode> {
        val loopEnterNode = createLoopEnterNode(loop)
        addNewSimpleNode(loopEnterNode)
        loopExitNodes.push(createLoopExitNode(loop))
        levelCounter++
        val blockEnterNode = createLoopBlockEnterNode(loop)
        addNewSimpleNode(blockEnterNode)
        // put block enter node twice so we can refer it after exit from loop condition
        lastNodes.push(blockEnterNode)
        loopEnterNodes.push(blockEnterNode)
        levelCounter++
        return loopEnterNode to blockEnterNode
    }

    fun enterDoWhileLoopCondition(loop: FirLoop): Pair<LoopBlockExitNode, LoopConditionEnterNode> {
        levelCounter--
        val blockExitNode = createLoopBlockExitNode(loop).also { addNewSimpleNode(it) }
        val conditionEnterNode = createLoopConditionEnterNode(loop.condition).also { addNewSimpleNode(it) }
        levelCounter++
        return blockExitNode to conditionEnterNode
    }

    fun exitDoWhileLoop(loop: FirLoop): Pair<LoopConditionExitNode, LoopExitNode> {
        loopEnterNodes.pop()
        levelCounter--
        val conditionExitNode = createLoopConditionExitNode(loop.condition)
        val conditionBooleanValue = conditionExitNode.booleanConstValue
        popAndAddEdge(conditionExitNode)
        val blockEnterNode = lastNodes.pop()
        require(blockEnterNode is LoopBlockEnterNode)
        addBackEdge(conditionExitNode, blockEnterNode, isDead = conditionBooleanValue == false)
        val loopExit = loopExitNodes.pop()
        addEdge(conditionExitNode, loopExit, propagateDeadness = false, isDead = conditionBooleanValue == true)
        loopExit.updateDeadStatus()
        lastNodes.push(loopExit)
        levelCounter--
        return conditionExitNode to loopExit
    }

    // ----------------------------------- Boolean operators -----------------------------------

    fun enterBinaryAnd(binaryLogicExpression: FirBinaryLogicExpression): BinaryAndEnterNode {
        assert(binaryLogicExpression.kind == LogicOperationKind.AND)
        binaryAndExitNodes.push(createBinaryAndExitNode(binaryLogicExpression))
        return createBinaryAndEnterNode(binaryLogicExpression).also { addNewSimpleNode(it) }.also { levelCounter++ }
    }

    fun exitLeftBinaryAndArgument(binaryLogicExpression: FirBinaryLogicExpression): Pair<BinaryAndExitLeftOperandNode, BinaryAndEnterRightOperandNode> {
        assert(binaryLogicExpression.kind == LogicOperationKind.AND)
        val lastNode = lastNodes.pop()
        val leftBooleanConstValue = lastNode.booleanConstValue

        val leftExitNode = createBinaryAndExitLeftOperandNode(binaryLogicExpression).also {
            addEdge(lastNode, it)
            addEdge(it, binaryAndExitNodes.top(), propagateDeadness = false, isDead = leftBooleanConstValue == true)
        }

        val rightEnterNode = createBinaryAndEnterRightOperandNode(binaryLogicExpression).also {
            addEdge(leftExitNode, it, isDead = leftBooleanConstValue == false)
            lastNodes.push(it)
        }
        return leftExitNode to rightEnterNode
    }

    fun exitBinaryAnd(binaryLogicExpression: FirBinaryLogicExpression): BinaryAndExitNode {
        levelCounter--
        assert(binaryLogicExpression.kind == LogicOperationKind.AND)
        return binaryAndExitNodes.pop().also {
            val rightNode = lastNodes.pop()
            addEdge(rightNode, it, propagateDeadness = false, isDead = it.leftOperandNode.booleanConstValue == false)
            it.updateDeadStatus()
            lastNodes.push(it)
        }
    }

    fun enterBinaryOr(binaryLogicExpression: FirBinaryLogicExpression): BinaryOrEnterNode {
        assert(binaryLogicExpression.kind == LogicOperationKind.OR)
        binaryOrExitNodes.push(createBinaryOrExitNode(binaryLogicExpression))
        return createBinaryOrEnterNode(binaryLogicExpression).also {
            addNewSimpleNode(it)
        }.also { levelCounter++ }
    }

    fun exitLeftBinaryOrArgument(binaryLogicExpression: FirBinaryLogicExpression): Pair<BinaryOrExitLeftOperandNode, BinaryOrEnterRightOperandNode> {
        levelCounter--
        assert(binaryLogicExpression.kind == LogicOperationKind.OR)
        val previousNode = lastNodes.pop()
        val leftBooleanValue = previousNode.booleanConstValue

        val leftExitNode = createBinaryOrExitLeftOperandNode(binaryLogicExpression).also {
            addEdge(previousNode, it)
            addEdge(it, binaryOrExitNodes.top(), propagateDeadness = false, isDead = leftBooleanValue == false)
        }

        val rightExitNode = createBinaryOrEnterRightOperandNode(binaryLogicExpression).also {
            addEdge(leftExitNode, it, propagateDeadness = true, isDead = leftBooleanValue == true)
            lastNodes.push(it)
            levelCounter++
        }
        return leftExitNode to rightExitNode
    }

    fun enterContract(qualifiedAccess: FirQualifiedAccess): EnterContractNode {
        return createEnterContractNode(qualifiedAccess).also { addNewSimpleNode(it) }
    }

    fun exitContract(qualifiedAccess: FirQualifiedAccess): ExitContractNode {
        return createExitContractNode(qualifiedAccess).also { addNewSimpleNode(it) }
    }

    fun exitBinaryOr(binaryLogicExpression: FirBinaryLogicExpression): BinaryOrExitNode {
        assert(binaryLogicExpression.kind == LogicOperationKind.OR)
        levelCounter--
        return binaryOrExitNodes.pop().also {
            val rightNode = lastNodes.pop()
            addEdge(rightNode, it, propagateDeadness = false)
            it.updateDeadStatus()
            lastNodes.push(it)
        }
    }

    private val CFGNode<*>.booleanConstValue: Boolean? get() = (fir as? FirConstExpression<*>)?.value as? Boolean?

    // ----------------------------------- Try-catch-finally -----------------------------------

    fun enterTryExpression(tryExpression: FirTryExpression): Pair<TryExpressionEnterNode, TryMainBlockEnterNode> {
        catchNodeStorages.push(NodeStorage())
        val enterTryExpressionNode = createTryExpressionEnterNode(tryExpression)
        addNewSimpleNode(enterTryExpressionNode)
        tryExitNodes.push(createTryExpressionExitNode(tryExpression))
        levelCounter++
        val enterTryNodeBlock = createTryMainBlockEnterNode(tryExpression)
        addNewSimpleNode(enterTryNodeBlock)
        addEdge(enterTryNodeBlock, exitTargetsForTry.top())

        for (catch in tryExpression.catches) {
            val catchNode = createCatchClauseEnterNode(catch)
            catchNodeStorage.push(catchNode)
            addEdge(enterTryNodeBlock, catchNode)
            addEdge(catchNode, exitTargetsForTry.top())
        }
        levelCounter++

        if (tryExpression.finallyBlock != null) {
            val finallyEnterNode = createFinallyBlockEnterNode(tryExpression)
            addEdge(enterTryNodeBlock, finallyEnterNode)
            finallyEnterNodes.push(finallyEnterNode)
        }

        return enterTryExpressionNode to enterTryNodeBlock
    }

    fun exitTryMainBlock(tryExpression: FirTryExpression): TryMainBlockExitNode {
        levelCounter--
        val node = createTryMainBlockExitNode(tryExpression)
        popAndAddEdge(node)
        addEdge(node, tryExitNodes.top())
        return node
    }

    fun enterCatchClause(catch: FirCatch): CatchClauseEnterNode {
        return catchNodeStorage[catch]!!.also { lastNodes.push(it) }.also { levelCounter++ }
    }

    fun exitCatchClause(catch: FirCatch): CatchClauseExitNode {
        levelCounter--
        return createCatchClauseExitNode(catch).also {
            popAndAddEdge(it)
            addEdge(it, tryExitNodes.top(), propagateDeadness = false)
        }
    }

    fun enterFinallyBlock(): FinallyBlockEnterNode {
        val enterNode = finallyEnterNodes.pop()
        lastNodes.push(enterNode)
        return enterNode
    }

    fun exitFinallyBlock(tryExpression: FirTryExpression): FinallyBlockExitNode {
        return createFinallyBlockExitNode(tryExpression).also {
            popAndAddEdge(it)
            addEdge(it, tryExitNodes.top())
        }
    }

    fun exitTryExpression(
        callCompleted: Boolean
    ): Pair<TryExpressionExitNode, UnionFunctionCallArgumentsNode?> {
        levelCounter--
        catchNodeStorages.pop()
        val node = tryExitNodes.pop()
        node.updateDeadStatus()
        lastNodes.push(node)
        val (_, unionNode) = processUnionOfArguments(node, callCompleted)
        return node to unionNode
    }

    // ----------------------------------- Resolvable call -----------------------------------

    fun exitQualifiedAccessExpression(qualifiedAccessExpression: FirQualifiedAccessExpression): QualifiedAccessNode {
        val returnsNothing = qualifiedAccessExpression.resultType.isNothing
        val node = createQualifiedAccessNode(qualifiedAccessExpression)
        if (returnsNothing) {
            addNodeThatReturnsNothing(node)
        } else {
            addNewSimpleNode(node)
        }
        return node
    }

    fun exitResolvedQualifierNode(resolvedQualifier: FirResolvedQualifier): ResolvedQualifierNode {
        return createResolvedQualifierNode(resolvedQualifier).also(this::addNewSimpleNode)
    }

    fun enterCall() {
        levelCounter++
    }

    fun exitIgnoredCall(functionCall: FirFunctionCall) {
        levelCounter--
        ignoredFunctionCalls += functionCall
    }

    fun exitFunctionCall(functionCall: FirFunctionCall, callCompleted: Boolean): Pair<FunctionCallNode, UnionFunctionCallArgumentsNode?> {
        val callWasIgnored = ignoredFunctionCalls.remove(functionCall)
        if (!callWasIgnored) {
            levelCounter--
        } else {
            ignoredFunctionCalls.clear()
        }
        val returnsNothing = functionCall.resultType.isNothing
        val node = createFunctionCallNode(functionCall)
        val (kind, unionNode) = processUnionOfArguments(node, callCompleted)
        if (returnsNothing) {
            addNodeThatReturnsNothing(node, preferredKind = kind)
        } else {
            addNewSimpleNode(node, preferredKind = kind)
        }
        return node to unionNode
    }

    fun exitDelegatedConstructorCall(
        call: FirDelegatedConstructorCall,
        callCompleted: Boolean
    ): Pair<DelegatedConstructorCallNode, UnionFunctionCallArgumentsNode?> {
        levelCounter--
        val node = createDelegatedConstructorCallNode(call)
        val (kind, unionNode) = processUnionOfArguments(node, callCompleted)
        addNewSimpleNode(node, preferredKind = kind)
        return node to unionNode
    }

    fun exitConstExpresion(constExpression: FirConstExpression<*>): ConstExpressionNode {
        return createConstExpressionNode(constExpression).also { addNewSimpleNode(it) }
    }

    fun exitVariableDeclaration(variable: FirProperty): VariableDeclarationNode {
        return createVariableDeclarationNode(variable).also { addNewSimpleNode(it) }
    }

    fun exitVariableAssignment(assignment: FirVariableAssignment): VariableAssignmentNode {
        return createVariableAssignmentNode(assignment).also { addNewSimpleNode(it) }
    }

    fun exitThrowExceptionNode(throwExpression: FirThrowExpression): ThrowExceptionNode {
        return createThrowExceptionNode(throwExpression).also { addNodeThatReturnsNothing(it) }
    }

    fun exitCheckNotNullCall(
        checkNotNullCall: FirCheckNotNullCall,
        callCompleted: Boolean
    ): Pair<CheckNotNullCallNode, UnionFunctionCallArgumentsNode?> {
        val node = createCheckNotNullCallNode(checkNotNullCall).also { addNewSimpleNode(it) }
        val unionNode = processUnionOfArguments(node, callCompleted).second
        return node to unionNode
    }

    /*
     * This is needed for some control flow constructions which are resolved as calls (when and elvis)
     * For usual call we have invariant that all arguments will be called before function call, but for
     *   when and elvis only one of arguments will be actually called, so it's illegal to pass data flow info
     *   from lambda in one of branches
     */
    private fun dropPostponedLambdasForNonDeterministicCalls() {
        exitsFromCompletedPostponedAnonymousFunctions.clear()
    }

    private fun processUnionOfArguments(
        node: CFGNode<*>,
        callCompleted: Boolean
    ): Pair<EdgeKind, UnionFunctionCallArgumentsNode?> {
        if (!shouldPassFlowFromInplaceLambda.top()) return EdgeKind.Forward to null
        var kind = EdgeKind.Forward
        if (!callCompleted || exitsFromCompletedPostponedAnonymousFunctions.isEmpty()) {
            return EdgeKind.Forward to null
        }
        val unionNode by lazy { createUnionFunctionCallArgumentsNode(node.fir) }
        var hasDirectPreviousNode = false
        var hasPostponedLambdas = false

        val iterator = exitsFromCompletedPostponedAnonymousFunctions.iterator()
        val lastPostponedLambdaExitNode = lastNode
        while (iterator.hasNext()) {
            val exitNode = iterator.next()
            if (node.level >= exitNode.level) continue
            hasPostponedLambdas = true
            if (exitNode == lastPostponedLambdaExitNode) {
                popAndAddEdge(node, preferredKind = EdgeKind.CfgForward)
                kind = EdgeKind.DfgForward
                hasDirectPreviousNode = true
            }
            addEdge(exitNode.lastPreviousNode, unionNode, preferredKind = EdgeKind.DfgForward)
            iterator.remove()
        }
        if (hasPostponedLambdas) {
            if (hasDirectPreviousNode) {
                lastNodes.push(unionNode)
            } else {
                addNewSimpleNode(unionNode)
            }
        } else {
            return EdgeKind.Forward to null
        }
        return Pair(kind, unionNode)
    }


    // ----------------------------------- Annotations -----------------------------------

    fun enterAnnotationCall(annotationCall: FirAnnotationCall): AnnotationEnterNode {
        val graph = ControlFlowGraph(null, "STUB_GRAPH_FOR_ANNOTATION_CALL", ControlFlowGraph.Kind.AnnotationCall)
        pushGraph(graph, Mode.Body)
        return createAnnotationEnterNode(annotationCall).also {
            lastNodes.push(it)
        }
    }

    fun exitAnnotationCall(annotationCall: FirAnnotationCall): AnnotationExitNode {
        val node = createAnnotationExitNode(annotationCall)
        popAndAddEdge(node)
        popGraph()
        return node
    }

    // ----------------------------------- Block -----------------------------------

    fun enterInitBlock(initBlock: FirAnonymousInitializer): Pair<InitBlockEnterNode, CFGNode<*>?> {
        // TODO: questionable moment that we should pass data flow from init to init

        val graph = ControlFlowGraph(initBlock, "init block", ControlFlowGraph.Kind.Function)
        pushGraph(graph, Mode.Body)
        val enterNode = createInitBlockEnterNode(initBlock).also {
            lastNodes.push(it)
        }
        val lastNode = runIf(lastNode is InitBlockExitNode) { lastNodes.pop() } ?: enterToLocalClassesMembers[initBlock.symbol]
        lastNode?.let { addEdge(it, enterNode, preferredKind = EdgeKind.DfgForward) }

        createInitBlockExitNode(initBlock).also {
            initBlockExitNodes.push(it)
            exitTargetsForTry.push(it)
        }

        return enterNode to lastNode
    }

    fun exitInitBlock(initBlock: FirAnonymousInitializer): Pair<InitBlockExitNode, ControlFlowGraph> {
        val exitNode = initBlockExitNodes.pop()
        require(exitNode == exitTargetsForTry.pop())
        popAndAddEdge(exitNode)
        val graph = popGraph()
        assert(graph.declaration == initBlock)
        exitNode.updateDeadStatus()
        return exitNode to graph
    }

    // ----------------------------------- Safe calls -----------------------------------

    fun enterSafeCall(safeCall: FirSafeCallExpression): EnterSafeCallNode {
        /*
         * We create
         *   lastNode -> enterNode
         *   lastNode -> exitNode
         * instead of
         *   lastNode -> enterNode -> exitNode
         * because of we need to fork flow on `enterNode`, so `exitNode`
         *   will have unchanged flow from `lastNode`
         */
        val lastNode = lastNodes.pop()
        val enterNode = createEnterSafeCallNode(safeCall)
        lastNodes.push(enterNode)
        val exitNode = createExitSafeCallNode(safeCall)
        exitSafeCallNodes.push(exitNode)
        addEdge(lastNode, enterNode)
        addEdge(lastNode, exitNode)
        return enterNode
    }

    fun exitSafeCall(): ExitSafeCallNode {
        return exitSafeCallNodes.pop().also {
            addNewSimpleNode(it)
            it.updateDeadStatus()
        }
    }

    // ----------------------------------- Elvis -----------------------------------

    fun exitElvisLhs(elvisExpression: FirElvisExpression): Triple<ElvisLhsExitNode, ElvisLhsIsNotNullNode, ElvisRhsEnterNode> {
        val exitNode = createElvisExitNode(elvisExpression).also {
            exitElvisExpressionNodes.push(it)
        }

        val lhsExitNode = createElvisLhsExitNode(elvisExpression).also {
            popAndAddEdge(it)
        }

        val lhsIsNotNullNode = createElvisLhsIsNotNullNode(elvisExpression).also {
            addEdge(lhsExitNode, it)
            addEdge(it, exitNode)
        }

        val rhsEnterNode = createElvisRhsEnterNode(elvisExpression).also {
            addEdge(lhsExitNode, it)
        }
        lastNodes.push(rhsEnterNode)
        return Triple(lhsExitNode, lhsIsNotNullNode, rhsEnterNode)
    }

    fun exitElvis(): ElvisExitNode {
        val exitNode = exitElvisExpressionNodes.pop()
        addNewSimpleNode(exitNode)
        exitNode.updateDeadStatus()
        dropPostponedLambdasForNonDeterministicCalls()
        return exitNode
    }

    // ----------------------------------- Contract description -----------------------------------

    fun enterContractDescription(): CFGNode<*> {
        pushGraph(ControlFlowGraph(null, "contract description", ControlFlowGraph.Kind.TopLevel), Mode.Body)

        return createContractDescriptionEnterNode().also {
            lastNodes.push(it)
        }
    }

    fun exitContractDescription() {
        lastNodes.pop()
        popGraph()
    }

    // -------------------------------------------------------------------------------------------------------------------------

    fun reset() {
        exitsOfAnonymousFunctions.clear()
        exitsFromCompletedPostponedAnonymousFunctions.clear()
        lastNodes.reset()
    }

    fun dropSubgraphFromCall(call: FirFunctionCall) {
        val graphs = mutableListOf<ControlFlowGraph>()

        call.acceptChildren(object : FirDefaultVisitorVoid() {
            override fun visitElement(element: FirElement) {
                element.acceptChildren(this)
            }

            override fun visitAnonymousFunction(anonymousFunction: FirAnonymousFunction) {
                anonymousFunction.controlFlowGraphReference?.accept(this)
            }

            override fun visitAnonymousObject(anonymousObject: FirAnonymousObject) {
                anonymousObject.controlFlowGraphReference?.accept(this)
            }

            override fun visitControlFlowGraphReference(controlFlowGraphReference: FirControlFlowGraphReference) {
                val graph = controlFlowGraphReference.controlFlowGraph ?: return
                if (graph.owner == null) return
                graphs += graph
            }
        }, null)

        for (graph in graphs) {
            currentGraph.removeSubGraph(graph)
        }
    }

    // ----------------------------------- Edge utils -----------------------------------

    private fun addNewSimpleNode(
        node: CFGNode<*>,
        isDead: Boolean = false,
        preferredKind: EdgeKind = EdgeKind.Forward
    ): CFGNode<*> {
        val lastNode = lastNodes.pop()
        addEdge(lastNode, node, isDead = isDead, preferredKind = preferredKind)
        lastNodes.push(node)
        return lastNode
    }

    private fun addNodeThatReturnsNothing(node: CFGNode<*>, preferredKind: EdgeKind = EdgeKind.Forward) {
        val exitNode: CFGNode<*> = exitTargetsForTry.top()
        addNodeWithJump(node, exitNode, preferredKind)
    }

    private fun addNodeWithJump(
        node: CFGNode<*>,
        targetNode: CFGNode<*>?,
        preferredKind: EdgeKind = EdgeKind.Forward,
        isBack: Boolean = false
    ) {
        popAndAddEdge(node, preferredKind)
        if (targetNode != null) {
            if (isBack) {
                addBackEdge(node, targetNode)
            } else {
                addEdge(node, targetNode, propagateDeadness = false)
            }
        }
        val stub = createStubNode()
        addEdge(node, stub)
        lastNodes.push(stub)
    }

    private fun popAndAddEdge(to: CFGNode<*>, preferredKind: EdgeKind = EdgeKind.Forward) {
        addEdge(lastNodes.pop(), to, preferredKind = preferredKind)
    }

    private fun addEdge(
        from: CFGNode<*>,
        to: CFGNode<*>,
        propagateDeadness: Boolean = true,
        isDead: Boolean = false,
        isBack: Boolean = false,
        preferredKind: EdgeKind = EdgeKind.Forward
    ) {
        val kind = if (isDead || from.isDead || to.isDead) {
            if (isBack) EdgeKind.DeadBackward else EdgeKind.DeadForward
        } else preferredKind
        CFGNode.addEdge(from, to, kind, propagateDeadness)
    }

    private fun addBackEdge(
        from: CFGNode<*>,
        to: CFGNode<*>,
        isDead: Boolean = false
    ) {
        addEdge(from, to, propagateDeadness = false, isDead = isDead, isBack = true, preferredKind = EdgeKind.CfgBackward)
    }

    // ----------------------------------- Utils -----------------------------------

    private inline fun Collection<FirSymbolOwner<*>>.forEachMember(block: (FirSymbolOwner<*>) -> Unit) {
        for (member in this) {
            for (callableDeclaration in member.unwrap()) {
                block(callableDeclaration)
            }
        }
    }

    private fun FirSymbolOwner<*>.unwrap(): List<FirSymbolOwner<*>> =
        when (this) {
            is FirFunction<*>, is FirAnonymousInitializer -> listOf(this)
            is FirProperty -> listOfNotNull(this.getter, this.setter, this)
            else -> emptyList()
        }

    private fun addNewSimpleNodeIfPossible(newNode: CFGNode<*>, isDead: Boolean = false): CFGNode<*>? {
        if (lastNodes.isEmpty) return null
        return addNewSimpleNode(newNode, isDead)
    }

}

fun FirDeclaration?.isLocalClassOrAnonymousObject() = ((this as? FirRegularClass)?.isLocal == true) || this is FirAnonymousObject