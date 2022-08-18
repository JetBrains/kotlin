/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.dfa.cfg

import org.jetbrains.kotlin.contracts.description.EventOccurrencesRange
import org.jetbrains.kotlin.contracts.description.isInPlace
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.hasExplicitBackingField
import org.jetbrains.kotlin.fir.declarations.utils.isLocal
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.builder.buildAnonymousFunctionExpression
import org.jetbrains.kotlin.fir.references.FirControlFlowGraphReference
import org.jetbrains.kotlin.fir.render
import org.jetbrains.kotlin.fir.resolve.dfa.*
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.resultType
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.util.ListMultimap
import org.jetbrains.kotlin.fir.util.listMultimapOf
import org.jetbrains.kotlin.fir.visitors.FirDefaultVisitor
import org.jetbrains.kotlin.utils.addToStdlib.popLast
import org.jetbrains.kotlin.utils.addToStdlib.runIf
import kotlin.random.Random

@RequiresOptIn
private annotation class CfgBuilderInternals

@OptIn(CfgInternals::class)
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
        Function, TopLevel, Body, ClassInitializer, PropertyInitializer, FieldInitializer
    }

    // ----------------------------------- Node caches -----------------------------------

    private val exitTargetsForReturn: SymbolBasedNodeStorage<FirFunction, FunctionExitNode> = SymbolBasedNodeStorage()
    private val exitTargetsForTry: Stack<CFGNode<*>> = stackOf()
    private val exitsOfAnonymousFunctions: MutableMap<FirFunctionSymbol<*>, FunctionExitNode> = mutableMapOf()
    private val enterToLocalClassesMembers: MutableMap<FirBasedSymbol<*>, CFGNode<*>?> = mutableMapOf()

    //return jumps via finally blocks, target -> jumps
    private val nonDirectJumps: ListMultimap<CFGNode<*>, CFGNode<*>> = listMultimapOf()

    private val postponedLambdas: MutableSet<FirFunctionSymbol<*>> = mutableSetOf()
    private val entersToPostponedAnonymousFunctions: MutableMap<FirFunctionSymbol<*>, PostponedLambdaEnterNode> = mutableMapOf()
    private val exitsFromPostponedAnonymousFunctions: MutableMap<FirFunctionSymbol<*>, PostponedLambdaExitNode> = mutableMapOf()
    private val parentGraphForAnonymousFunctions: MutableMap<FirFunctionSymbol<*>, ControlFlowGraph> = mutableMapOf()

    private val loopEnterNodes: NodeStorage<FirElement, CFGNode<FirElement>> = NodeStorage()
    private val loopExitNodes: NodeStorage<FirLoop, LoopExitNode> = NodeStorage()

    private val exitsFromCompletedPostponedAnonymousFunctions: MutableList<MutableList<CFGNode<*>>> = mutableListOf()

    private val whenExitNodes: NodeStorage<FirWhenExpression, WhenExitNode> = NodeStorage()
    private val whenBranchIndices: Stack<Map<FirWhenBranch, Int>> = stackOf()

    private val binaryAndExitNodes: Stack<BinaryAndExitNode> = stackOf()
    private val binaryOrExitNodes: Stack<BinaryOrExitNode> = stackOf()

    private val tryExitNodes: NodeStorage<FirTryExpression, TryExpressionExitNode> = NodeStorage()
    private val tryMainExitNodes: NodeStorage<FirTryExpression, TryMainBlockExitNode> = NodeStorage()
    private val catchNodeStorages: Stack<NodeStorage<FirCatch, CatchClauseEnterNode>> = stackOf()
    private val catchNodeStorage: NodeStorage<FirCatch, CatchClauseEnterNode> get() = catchNodeStorages.top()
    private val catchExitNodeStorages: Stack<NodeStorage<FirCatch, CatchClauseExitNode>> = stackOf()
    private val finallyEnterNodes: Stack<FinallyBlockEnterNode> = stackOf()
    private val finallyExitNodes: NodeStorage<FirTryExpression, FinallyBlockExitNode> = NodeStorage()

    private val initBlockExitNodes: Stack<InitBlockExitNode> = stackOf()

    private val exitSafeCallNodes: Stack<ExitSafeCallNode> = stackOf()
    private val exitElvisExpressionNodes: Stack<ElvisExitNode> = stackOf()
    private val elvisRhsEnterNodes: Stack<ElvisRhsEnterNode> = stackOf()

    private val notCompletedFunctionCalls: Stack<MutableList<FunctionCallNode>> = stackOf()

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

    fun isThereControlFlowInfoForAnonymousFunction(function: FirAnonymousFunction): Boolean =
        function.controlFlowGraphReference?.controlFlowGraph != null ||
                exitsOfAnonymousFunctions.containsKey(function.symbol)

    // This function might throw exception if !isThereControlFlowInfoForAnonymousFunction(function)
    fun returnExpressionsOfAnonymousFunction(function: FirAnonymousFunction): Collection<FirStatement> {
        fun FirElement.extractArgument(): FirElement = when {
            this is FirReturnExpression && target.labeledElement.symbol == function.symbol -> result.extractArgument()
            else -> this
        }

        fun CFGNode<*>.extractArgument(): FirElement? = when (this) {
            is FunctionEnterNode, is TryMainBlockEnterNode, is FinallyBlockExitNode, is CatchClauseEnterNode -> null
            is BlockExitNode -> if (function.isLambda || isDead) firstPreviousNode.extractArgument() else null
            is StubNode -> firstPreviousNode.extractArgument()
            else -> fir.extractArgument()
        }

        val exitNode = function.controlFlowGraphReference?.controlFlowGraph?.exitNode
            ?: exitsOfAnonymousFunctions.getValue(function.symbol)
        val nonDirect = nonDirectJumps[exitNode]
        return (nonDirect + exitNode.previousNodes).mapNotNullTo(mutableSetOf()) {
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

    fun enterFunction(function: FirFunction): Triple<FunctionEnterNode, LocalFunctionDeclarationNode?, CFGNode<*>?> {
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
            if (localFunctionNode == previousNode) {
                addEdge(localFunctionNode, enterNode, preferredKind = EdgeKind.Forward)
            } else {
                addEdge(previousNode, enterNode, preferredKind = EdgeKind.DfgForward)
            }
        }

        createFunctionExitNode(function).also {
            exitTargetsForReturn.push(it)
            exitTargetsForTry.push(it)
        }

        return Triple(enterNode, localFunctionNode, previousNode)
    }

    fun exitFunction(function: FirFunction): Pair<FunctionExitNode, ControlFlowGraph> {
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

    fun visitPostponedAnonymousFunction(anonymousFunctionExpression: FirAnonymousFunctionExpression): Pair<PostponedLambdaEnterNode, PostponedLambdaExitNode> {
        val anonymousFunction = anonymousFunctionExpression.anonymousFunction
        val enterNode = createPostponedLambdaEnterNode(anonymousFunction)
        val exitNode = createPostponedLambdaExitNode(anonymousFunctionExpression)
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
            val lambdaExpression = buildAnonymousFunctionExpression {
                source = anonymousFunction.source
                this.anonymousFunction = anonymousFunction
            }
            val exitFromLambda = createPostponedLambdaExitNode(lambdaExpression).also {
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
            if (!invocationKind.isInPlace) {
                exitTargetsForTry.push(it)
            }
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
            EventOccurrencesRange.AT_LEAST_ONCE, EventOccurrencesRange.MORE_THAN_ONCE, EventOccurrencesRange.UNKNOWN -> true
            else -> false
        }

    fun exitAnonymousFunction(anonymousFunction: FirAnonymousFunction): Triple<FunctionExitNode, PostponedLambdaExitNode?, ControlFlowGraph> {
        val symbol = anonymousFunction.symbol
        val exitNode = exitsOfAnonymousFunctions.remove(symbol)!!.also {
            require(it == exitTargetsForReturn.pop())
            if (!anonymousFunction.invocationKind.isInPlace) {
                require(it == exitTargetsForTry.pop())
            }
        }
        popAndAddEdge(exitNode)
        exitNode.updateDeadStatus()

        val graph = popGraph().also { graph ->
            assert(graph.declaration == anonymousFunction)
            assert(graph.exitNode == exitNode)
            // TODO: disregarding the edges is probably not correct, though this should never find any nodes anyway
            exitsFromCompletedPostponedAnonymousFunctions.lastOrNull()?.removeAll { it.owner == graph }
        }

        val postponedEnterNode = entersToPostponedAnonymousFunctions.remove(symbol)!!
        val postponedExitNode = exitsFromPostponedAnonymousFunctions.remove(symbol)!!

        val lambdaIsPostponedFromCall = postponedLambdas.remove(symbol)
        if (!lambdaIsPostponedFromCall) {
            lastNodes.push(postponedExitNode)
        }

        val invocationKind = anonymousFunction.invocationKind
        val maybeNonZero = invocationKind != null && invocationKind != EventOccurrencesRange.ZERO
        val maybeZero = invocationKind != EventOccurrencesRange.AT_LEAST_ONCE &&
                invocationKind != EventOccurrencesRange.EXACTLY_ONCE &&
                invocationKind != EventOccurrencesRange.MORE_THAN_ONCE
        // Four cases we handle differently here:
        //  1. function not called in-place: data and control flow skips the function,
        //     and any assignment inside the function invalidates smart casts
        //  2. function never executed (EventOccurrencesRange.ZERO): same as above,
        //     but without smart cast invalidation
        //  3. function executed at least once (or exactly once or more than once):
        //     control flow merged from postponedEnterNode and exitNode into postponedExitNode,
        //     data flow goes from exitNode to the union of arguments of the call
        //  4. function executed an unknown number of times (maybe zero, maybe not):
        //     both control flow and data flow merged from postponedEnterNode and exitNode
        //     into postponedExitNode
        if (maybeZero) {
            val kind = if (postponedExitNode.isDead) EdgeKind.DeadForward else EdgeKind.CfgForward
            CFGNode.addJustKindEdge(postponedEnterNode, postponedExitNode, kind, propagateDeadness = true)
        }
        if (maybeNonZero) {
            addEdge(exitNode, postponedExitNode, preferredKind = if (maybeZero) EdgeKind.Forward else EdgeKind.CfgForward)
            if (!maybeZero && shouldPassFlowFromInplaceLambda.top()) {
                exitsFromCompletedPostponedAnonymousFunctions.lastOrNull()?.add(postponedExitNode)
            }
        }

        val containingGraph = parentGraphForAnonymousFunctions.remove(symbol) ?: currentGraph
        containingGraph.addSubGraph(graph)
        return if (lambdaIsPostponedFromCall && !(maybeNonZero && maybeZero)) {
            Triple(exitNode, null, graph)
        } else {
            Triple(exitNode, postponedExitNode, graph)
        }
    }

    fun exitAnonymousFunctionExpression(anonymousFunctionExpression: FirAnonymousFunctionExpression): AnonymousFunctionExpressionExitNode {
        return createAnonymousFunctionExpressionExitNode(anonymousFunctionExpression).also {
            addNewSimpleNode(it)
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

    fun exitClass(klass: FirClass): ControlFlowGraph {
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
                is FirField -> declaration.controlFlowGraphReference?.controlFlowGraph
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

    fun prepareForLocalClassMembers(members: Collection<FirDeclaration>) {
        members.forEachMember {
            enterToLocalClassesMembers[it.symbol] = lastNodes.topOrNull()
        }
    }

    fun cleanAfterForLocalClassMembers(members: Collection<FirDeclaration>) {
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

    fun enterAnonymousObject(anonymousObject: FirAnonymousObject): AnonymousObjectEnterNode {
        val enterNode = createAnonymousObjectEnterNode(anonymousObject)
        // TODO: looks like there was some problem with enum initializers that causes `lastNodes` to be empty
        lastNodes.popOrNull()?.let { addEdge(it, enterNode, preferredKind = EdgeKind.Forward) }
        lastNodes.push(enterNode)
        enterClass()
        return enterNode
    }

    fun exitAnonymousObject(anonymousObject: FirAnonymousObject): Pair<AnonymousObjectExitNode, ControlFlowGraph> {
        val graph = exitClass(anonymousObject).also {
            currentGraph.addSubGraph(it)
        }
        val enterNode = lastNodes.popOrNull()
        if (enterNode !is AnonymousObjectEnterNode) {
            throw AssertionError("anonymous object exit should be preceded by anonymous object enter, but got $enterNode")
        }
        val exitNode = createAnonymousObjectExitNode(anonymousObject)
        // TODO: Intentionally not using anonymous object init blocks for data flow? Might've been a FE1.0 bug.
        addEdge(enterNode, graph.enterNode, preferredKind = EdgeKind.CfgForward)
        if (!graph.exitNode.isDead) {
            addEdge(graph.exitNode, exitNode, preferredKind = EdgeKind.CfgForward)
        }
        addEdge(enterNode, exitNode, preferredKind = EdgeKind.DfgForward)
        // TODO: Here we're assuming that the methods are called after the object is constructed, which is really not true
        //   (init blocks can call them). But FE1.0 did so too, hence the following code compiles and prints 0:
        //     val x: Int
        //     object {
        //         fun bar() = x
        //         init { x = bar() }
        //     }
        //     println(x)
        visitLocalClassFunctions(anonymousObject, exitNode)
        lastNodes.push(exitNode)
        return exitNode to graph
    }

    fun exitAnonymousObjectExpression(anonymousObjectExpression: FirAnonymousObjectExpression): AnonymousObjectExpressionExitNode {
        return createAnonymousObjectExpressionExitNode(anonymousObjectExpression).also {
            addNewSimpleNodeIfPossible(it)
        }
    }

    private fun visitLocalClassFunctions(klass: FirClass, node: CFGNodeWithSubgraphs<*>) {
        klass.declarations.filterIsInstance<FirFunction>().forEach { function ->
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
        if (property.initializer == null && property.delegate == null && !property.hasExplicitBackingField) return null

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
        if (property.initializer == null && property.delegate == null && !property.hasExplicitBackingField) return null
        val exitNode = exitTargetsForTry.pop() as PropertyInitializerExitNode
        popAndAddEdge(exitNode)
        val graph = popGraph()
        assert(exitNode == graph.exitNode)
        return exitNode to graph
    }

    // ----------------------------------- Field -----------------------------------

    fun enterField(field: FirField): FieldInitializerEnterNode? {
        if (field.initializer == null) return null

        val graph = ControlFlowGraph(field, "val ${field.name}", ControlFlowGraph.Kind.FieldInitializer)
        pushGraph(graph, Mode.FieldInitializer)

        val enterNode = createFieldInitializerEnterNode(field)
        val exitNode = createFieldInitializerExitNode(field)
        exitTargetsForTry.push(exitNode)

        enterToLocalClassesMembers[field.symbol]?.let {
            addEdge(it, enterNode, preferredKind = EdgeKind.DfgForward)
        }

        lastNodes.push(enterNode)
        return enterNode
    }

    fun exitField(field: FirField): Pair<FieldInitializerExitNode, ControlFlowGraph>? {
        if (field.initializer == null) return null
        val exitNode = exitTargetsForTry.pop() as FieldInitializerExitNode
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

        val labelForFinallyBLock = when (jump) {
            is FirReturnExpression -> ReturnPath(jump.target.labeledElement.symbol)
            is FirLoopJump -> LoopPath(jump)
            else -> NormalPath
        }

        addNodeWithJump(
            node,
            nextNode,
            isBack = jump is FirContinueExpression,
            trackJump = jump is FirReturnExpression,
            label = NormalPath,
            labelForFinallyBLock = labelForFinallyBLock
        )
        return node
    }

    // ----------------------------------- When -----------------------------------

    fun enterWhenExpression(whenExpression: FirWhenExpression): WhenEnterNode {
        val node = createWhenEnterNode(whenExpression)
        addNewSimpleNode(node)
        whenExitNodes.push(createWhenExitNode(whenExpression))
        whenBranchIndices.push(whenExpression.branches.mapIndexed { index, branch -> branch to index }.toMap())
        notCompletedFunctionCalls.push(mutableListOf())
        levelCounter++
        splitDataFlowForPostponedLambdas()
        return node
    }

    fun enterWhenBranchCondition(whenBranch: FirWhenBranch): WhenBranchConditionEnterNode {
        levelCounter += whenBranchIndices.top().getValue(whenBranch)
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
        levelCounter -= whenBranchIndices.top().getValue(whenBranch)
        return node
    }

    fun exitWhenExpression(
        whenExpression: FirWhenExpression
    ): Triple<WhenExitNode, WhenSyntheticElseBranchNode?, MergePostponedLambdaExitsNode?> {
        val whenExitNode = whenExitNodes.pop()
        // exit from last condition node still on stack
        // we should remove it
        notCompletedFunctionCalls.pop().forEach(::completeFunctionCall)
        val lastWhenConditionExit = lastNodes.pop()
        val syntheticElseBranchNode = if (!whenExpression.isProperlyExhaustive) {
            createWhenSyntheticElseBranchNode(whenExpression).apply {
                addEdge(lastWhenConditionExit, this)
                addEdge(this, whenExitNode)
            }
        } else null
        whenExitNode.updateDeadStatus()
        lastNodes.push(whenExitNode)
        levelCounter--
        whenBranchIndices.pop()
        return Triple(whenExitNode, syntheticElseBranchNode, joinDataFlowFromPostponedLambdasWith(whenExitNode))
    }

    // ----------------------------------- While Loop -----------------------------------

    fun enterWhileLoop(loop: FirLoop): Pair<LoopEnterNode, LoopConditionEnterNode> {
        val loopEnterNode = createLoopEnterNode(loop).also {
            addNewSimpleNode(it)
            loopEnterNodes.push(it)
        }
        loopExitNodes.push(createLoopExitNode(loop))
        levelCounter++
        val conditionEnterNode = createLoopConditionEnterNode(loop.condition, loop).also {
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
            addBackEdge(loopBlockExitNode, conditionEnterNode, label = LoopBackPath)
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
        val conditionEnterNode = createLoopConditionEnterNode(loop.condition, loop).also { addNewSimpleNode(it) }
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
        addBackEdge(conditionExitNode, blockEnterNode, isDead = conditionBooleanValue == false, label = LoopBackPath)
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
        catchExitNodeStorages.push(NodeStorage())
        val enterTryExpressionNode = createTryExpressionEnterNode(tryExpression)
        addNewSimpleNode(enterTryExpressionNode)
        val tryExitNode = createTryExpressionExitNode(tryExpression)
        tryExitNodes.push(tryExitNode)
        levelCounter++
        val enterTryNodeBlock = createTryMainBlockEnterNode(tryExpression)
        addNewSimpleNode(enterTryNodeBlock)

        val exitTryNodeBlock = createTryMainBlockExitNode(tryExpression)
        tryMainExitNodes.push(exitTryNodeBlock)

        for (catch in tryExpression.catches) {
            val catchNode = createCatchClauseEnterNode(catch)
            catchNodeStorage.push(catchNode)
            // a flow where an exception of interest is thrown and caught before executing any of try-main block.
            addEdge(enterTryExpressionNode, catchNode)
        }
        levelCounter++

        if (tryExpression.finallyBlock != null) {
            val finallyEnterNode = createFinallyBlockEnterNode(tryExpression)
            // a flow where an uncaught exception is thrown before executing any of try-main block.
            addEdge(enterTryExpressionNode, finallyEnterNode, propagateDeadness = false, label = UncaughtExceptionPath)
            finallyEnterNodes.push(finallyEnterNode)
            finallyExitNodes.push(createFinallyBlockExitNode(tryExpression))
        }
        notCompletedFunctionCalls.push(mutableListOf())
        splitDataFlowForPostponedLambdas()
        return enterTryExpressionNode to enterTryNodeBlock
    }

    fun exitTryMainBlock(): TryMainBlockExitNode {
        levelCounter--
        val node = tryMainExitNodes.top()
        popAndAddEdge(node)
        node.updateDeadStatus()
        val finallyEnterNode = finallyEnterNodes.topOrNull()
        // NB: Check the level to avoid adding an edge to the finally block at an upper level.
        if (finallyEnterNode != null && finallyEnterNode.level == levelCounter + 1) {
            // TODO: in case of return/continue/break in try main block, we need a unique label.
            addEdge(node, finallyEnterNode)
            //in case try exit is dead, but there is other edges to finally (eg return)
            // actually finallyEnterNode can't be dead, except for the case when the whole try is dead
            finallyEnterNode.updateDeadStatus()
        } else {
            addEdge(node, tryExitNodes.top(), propagateDeadness = false)
        }
        return node
    }

    fun enterCatchClause(catch: FirCatch): CatchClauseEnterNode {
        return catchNodeStorage[catch]!!.also {
            val tryMainExitNode = tryMainExitNodes.top()
            // a flow where an exception of interest is thrown and caught after executing all of try-main block.
            addEdge(tryMainExitNode, it)
            //tryMainExitNode might be dead (eg main block contains return), but it doesn't mean catch block is also dead
            it.updateDeadStatus()
            val finallyEnterNode = finallyEnterNodes.topOrNull()
            // a flow where an uncaught exception is thrown before executing any of catch clause.
            // NB: Check the level to avoid adding an edge to the finally block at an upper level.
            if (finallyEnterNode != null && finallyEnterNode.level == levelCounter + 1) {
                addEdge(it, finallyEnterNode, propagateDeadness = false, label = UncaughtExceptionPath)
            } else {
                addEdge(it, exitTargetsForTry.top(), label = UncaughtExceptionPath)
            }
            lastNodes.push(it)
            levelCounter++
        }
    }

    fun exitCatchClause(catch: FirCatch): CatchClauseExitNode {
        levelCounter--
        return createCatchClauseExitNode(catch).also {
            popAndAddEdge(it)
            val finallyEnterNode = finallyEnterNodes.topOrNull()
            // NB: Check the level to avoid adding an edge to the finally block at an upper level.
            if (finallyEnterNode != null && finallyEnterNode.level == levelCounter + 1) {
                // TODO: in case of return/continue/break in catch clause, we need a unique label.
                addEdge(it, finallyEnterNode, propagateDeadness = false)
            } else {
                addEdge(it, tryExitNodes.top(), propagateDeadness = false)
            }
            catchExitNodeStorages.top().push(it)
        }
    }

    fun enterFinallyBlock(): FinallyBlockEnterNode {
        val enterNode = finallyEnterNodes.pop()
        lastNodes.push(enterNode)
        return enterNode
    }

    fun exitFinallyBlock(): FinallyBlockExitNode {
        return finallyExitNodes.pop().also { finallyExit ->
            popAndAddEdge(finallyExit)
            val tryExitNode = tryExitNodes.top()
            // a flow where either there wasn't any exception or caught if any.
            addEdge(finallyExit, tryExitNode)
            if (finallyExit.isDead) {
                //refresh forward links, which were created before finalizing try expression (eg created by `break`)
                propagateDeadnessForward(finallyExit)
            }
            // a flow that exits to the exit target while there was an uncaught exception.
            //todo this edge might exist already if try has jump outside, so we effectively lose labeled edge here
            addEdgeIfNotExist(finallyExit, exitTargetsForTry.top(), propagateDeadness = false, label = UncaughtExceptionPath)
            // TODO: differentiate flows that return/(re)throw in try main block and/or catch clauses
            //   To do so, we need mappings from such distinct label to original exit target (fun exit or loop)
            //   Also, CFG should support multiple edges towards the same destination node
        }
    }

    fun exitTryExpression(
        callCompleted: Boolean
    ): Pair<TryExpressionExitNode, UnionFunctionCallArgumentsNode?> {
        levelCounter--
        catchNodeStorages.pop()
        val catchExitNodes = catchExitNodeStorages.pop()
        val tryMainExitNode = tryMainExitNodes.pop()

        notCompletedFunctionCalls.pop().forEach(::completeFunctionCall)

        val node = tryExitNodes.pop()
        node.updateDeadStatus()
        lastNodes.push(node)
        val (_, unionNode) = processUnionOfArguments(node, callCompleted)

        val allCatchesAreDead = tryMainExitNode.fir.catches.all { catch -> catchExitNodes[catch]!!.isDead }
        val tryMainBlockIsDead = tryMainExitNode.previousNodes.all { prev ->
            prev.isDead || tryMainExitNode.incomingEdges.getValue(prev).label != NormalPath
        }
        if (tryMainBlockIsDead && allCatchesAreDead) {
            //if try expression doesn't have any regular non-dead exits, we add stub to make everything after dead
            val stub = createStubNode()
            popAndAddEdge(stub)
            lastNodes.push(stub)
        }

        return node to unionNode
    }

    //this is a workaround to make function call dead when call is completed _after_ building its node in the graph
    //this happens when completing the last call in try/catch blocks
    //todo this doesn't make fully 'right' Nothing node (doesn't support going to catch and pass through finally)
    // because doing those afterwards is quite challenging
    // it would be much easier if we could build calls after full completion only, at least for Nothing calls
    private fun completeFunctionCall(node: FunctionCallNode) {
        if (!node.fir.resultType.isNothing) return
        val stub = withLevelOfNode(node) { createStubNode() }
        val edges = node.followingNodes.map { it to node.outgoingEdges.getValue(it) }
        CFGNode.removeAllOutgoingEdges(node)
        addEdge(node, stub)
        for ((to, edge) in edges) {
            addEdge(
                from = stub,
                to = to,
                isBack = edge.kind.isBack,
                preferredKind = edge.kind,
                label = edge.label
            )
        }
        stub.followingNodes.forEach { propagateDeadnessForward(it, deep = true) }
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

    fun exitSmartCastExpression(smartCastExpression: FirSmartCastExpression): SmartCastExpressionExitNode {
        val returnsNothing = smartCastExpression.resultType.isNothing
        val node = createSmartCastExitNode(smartCastExpression)
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
        splitDataFlowForPostponedLambdas()
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
        if (!returnsNothing && !callCompleted) {
            notCompletedFunctionCalls.topOrNull()?.add(node)
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

    fun exitStringConcatenationCall(call: FirStringConcatenationCall): Pair<StringConcatenationCallNode, UnionFunctionCallArgumentsNode?> {
        levelCounter--
        val node = createStringConcatenationCallNode(call)
        val (kind, unionNode) = processUnionOfArguments(node, true)
        addNewSimpleNode(node, preferredKind = kind)
        return node to unionNode
    }

    fun exitConstExpression(constExpression: FirConstExpression<*>): ConstExpressionNode {
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
        levelCounter--
        val node = createCheckNotNullCallNode(checkNotNullCall)
        if (checkNotNullCall.resultType.isNothing) {
            addNodeThatReturnsNothing(node)
        } else {
            addNewSimpleNode(node)
        }
        val unionNode = processUnionOfArguments(node, callCompleted).second
        return node to unionNode
    }

    // Arguments are evaluated left to right, and this is how data flows.
    //    foo(run { x as String; 1 }, { /* x smartcasted to String */ x.length })
    //
    // However, as we need to fix type parameters before analyzing lambdas, this is not always the order of analysis;
    // if that is possible, multiple lambdas should be considered to be concurrent.
    //
    //    foo(run { x as String; genericFunction() }, run { /* x not smartcastable because this lambda may be resolved first */ 1 })
    //    /* x is smartcastable after the call */
    //
    // And if the lambda is conditional, then the data flow needs to be merged with other branches.
    //
    //    foo(nullable?.let { x as String; genericFunction() }, run { 1 })
    //    /* x is not smartcastable */
    //
    //    foo(nullable ?: run { x as String; genericFunction() }, run { 1 })
    //    /* x is not smartcastable */
    //
    //    foo(if (condition) run { x as String; genericFunction() } else { genericFunction() }, run { 1 })
    //    /* x is not smartcastable */
    //
    //    foo(if (condition) run { x as String; genericFunction() } else { x as String; genericFunction() }, run { 1 })
    //    /* x is smartcastable */
    //
    // `splitDataFlowForPostponedLambdas` in `enterX` should be matched with either `joinDataFlowFromPostponedLambdasWith`
    // or `processUnionOfArguments` in `exitX`. The difference is that the latter creates an intersection of all the lambdas'
    // type information (like after function calls - all casts from all lambdas are valid) while the former is a union
    // (like after `if` - only the casts from one of the lambdas are valid, and we don't know which).
    //
    private fun splitDataFlowForPostponedLambdas() {
        exitsFromCompletedPostponedAnonymousFunctions.add(mutableListOf())
    }

    private fun joinDataFlowFromPostponedLambdasWith(node: CFGNode<*>): MergePostponedLambdaExitsNode? {
        val currentLevelExits = exitsFromCompletedPostponedAnonymousFunctions.popLast()
        if (currentLevelExits.isEmpty()) {
            return null
        }

        val joinNode = createMergePostponedLambdaExitsNode(node.fir)
        addEdge(node, joinNode)
        currentLevelExits.joinDataFlowFromPostponedLambdasTo(joinNode)
        exitsFromCompletedPostponedAnonymousFunctions.lastOrNull()?.add(joinNode)
        return joinNode
    }

    private fun MutableList<CFGNode<*>>.joinDataFlowFromPostponedLambdasTo(node: CFGNode<*>) {
        for (exitNode in this) {
            // To avoid storing nodes from subgraphs in the list, we have PostponedLambdaExitNode instead of the real
            // exit node of the lambda subgraph. The latter is the previous node of the former. Everything else is
            // already a join/union node in this graph.
            val functionExitOrMerge = if (exitNode is PostponedLambdaExitNode) exitNode.lastPreviousNode else exitNode
            addEdge(functionExitOrMerge, node, preferredKind = EdgeKind.DfgForward)
        }
    }

    private fun processUnionOfArguments(
        node: CFGNode<*>,
        callCompleted: Boolean
    ): Pair<EdgeKind, UnionFunctionCallArgumentsNode?> {
        val currentLevelExits = exitsFromCompletedPostponedAnonymousFunctions.popLast()
        if (currentLevelExits.isEmpty()) {
            return EdgeKind.Forward to null
        }

        if (!callCompleted || !shouldPassFlowFromInplaceLambda.top()) {
            currentLevelExits.singleOrNull()?.let {
                exitsFromCompletedPostponedAnonymousFunctions.lastOrNull()?.add(it)
                return EdgeKind.Forward to null
            }

            val unionNode = createUnionFunctionCallArgumentsNode(node.fir)
            currentLevelExits.joinDataFlowFromPostponedLambdasTo(unionNode)
            exitsFromCompletedPostponedAnonymousFunctions.lastOrNull()?.addAll(currentLevelExits)
            return EdgeKind.Forward to unionNode
        }

        val unionNode = createUnionFunctionCallArgumentsNode(node.fir)
        currentLevelExits.joinDataFlowFromPostponedLambdasTo(unionNode)

        if (lastNode in currentLevelExits) {
            popAndAddEdge(node, preferredKind = EdgeKind.CfgForward)
            lastNodes.push(unionNode)
            return EdgeKind.DfgForward to unionNode
        }
        addNewSimpleNode(unionNode)
        return EdgeKind.Forward to unionNode
    }

    fun exitWhenSubjectExpression(expression: FirWhenSubjectExpression): WhenSubjectExpressionExitNode {
        return createWhenSubjectExpressionExitNode(expression).also { addNewSimpleNode(it) }
    }

    // ----------------------------------- Annotations -----------------------------------

    fun enterAnnotation(annotation: FirAnnotation): AnnotationEnterNode {
        val graph = ControlFlowGraph(null, "STUB_GRAPH_FOR_ANNOTATION_CALL", ControlFlowGraph.Kind.AnnotationCall)
        pushGraph(graph, Mode.Body)
        return createAnnotationEnterNode(annotation).also {
            lastNodes.push(it)
        }
    }

    fun exitAnnotation(annotation: FirAnnotation): AnnotationExitNode {
        val node = createAnnotationExitNode(annotation)
        popAndAddEdge(node)
        popGraph()
        return node
    }

    // ----------------------------------- Callable references -----------------------------------

    fun exitCallableReference(callableReferenceAccess: FirCallableReferenceAccess): CallableReferenceNode {
        return createCallableReferenceNode(callableReferenceAccess).also { addNewSimpleNode(it) }
    }

    fun exitGetClassCall(getClassCall: FirGetClassCall): GetClassCallNode {
        return createGetClassCallNode(getClassCall).also { addNewSimpleNode(it) }
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
         * because we need to fork flow before `enterNode`, so `exitNode`
         *   will have unchanged flow from `lastNode`
         *   which corresponds to a path with nullable receiver.
         */
        val lastNode = lastNodes.pop()
        val enterNode = createEnterSafeCallNode(safeCall)
        lastNodes.push(enterNode)
        val exitNode = createExitSafeCallNode(safeCall)
        exitSafeCallNodes.push(exitNode)
        addEdge(lastNode, enterNode)
        if (elvisRhsEnterNodes.topOrNull()?.fir?.lhs === safeCall) {
            //if this is safe call in lhs of elvis, we make two edges
            // 1. Df-only edge to exit node, to get not null implications there
            // 2. Cf-only edge to elvis rhs
            addEdge(lastNode, exitNode, preferredKind = EdgeKind.DfgForward)
            addEdge(lastNode, elvisRhsEnterNodes.top(), preferredKind = EdgeKind.CfgForward)
        } else {
            addEdge(lastNode, exitNode)
        }
        splitDataFlowForPostponedLambdas()
        return enterNode
    }

    fun exitSafeCall(): Pair<ExitSafeCallNode, MergePostponedLambdaExitsNode?> {
        // There will be two paths towards this exit safe call node:
        // one from the node prior to the enclosing safe call, and
        // the other from the selector part in the enclosing safe call.
        // Note that *neither* points to the safe call directly.
        // So, when it comes to the real exit of the enclosing block/function,
        // the safe call bound to this exit safe call node should be retrieved.
        return exitSafeCallNodes.pop().let {
            addNewSimpleNode(it)
            it.updateDeadStatus()
            it to joinDataFlowFromPostponedLambdasWith(it)
        }
    }

    // ----------------------------------- Elvis -----------------------------------

    fun enterElvis(elvisExpression: FirElvisExpression) {
        elvisRhsEnterNodes.push(createElvisRhsEnterNode(elvisExpression))
        splitDataFlowForPostponedLambdas()
    }

    fun exitElvisLhs(elvisExpression: FirElvisExpression): Triple<ElvisLhsExitNode, ElvisLhsIsNotNullNode, ElvisRhsEnterNode> {
        val exitNode = createElvisExitNode(elvisExpression).also {
            exitElvisExpressionNodes.push(it)
        }

        val typedFir = lastNodes.topOrNull()?.fir as? FirExpression
        val type = typedFir?.typeRef?.coneTypeSafe<ConeKotlinType>()

        val lhsExitNode = createElvisLhsExitNode(elvisExpression).also {
            popAndAddEdge(it)
        }

        val lhsIsNotNullNode = createElvisLhsIsNotNullNode(elvisExpression).also {
            val preferredKind = if (type?.isNullableNothing == true) {
                EdgeKind.DeadForward
            } else {
                EdgeKind.Forward
            }

            addEdge(lhsExitNode, it, preferredKind = preferredKind)
            addEdge(it, exitNode, propagateDeadness = false)
        }

        val rhsEnterNode = elvisRhsEnterNodes.pop().also {
            addEdge(lhsExitNode, it)
        }
        lastNodes.push(rhsEnterNode)
        return Triple(lhsExitNode, lhsIsNotNullNode, rhsEnterNode)
    }

    fun exitElvis(): Pair<ElvisExitNode, MergePostponedLambdaExitsNode?> {
        val exitNode = exitElvisExpressionNodes.pop()
        addNewSimpleNode(exitNode)
        exitNode.updateDeadStatus()
        return exitNode to joinDataFlowFromPostponedLambdasWith(exitNode)
    }

    // ----------------------------------- Contract description -----------------------------------

    fun enterContractDescription(): CFGNode<*> {
        pushGraph(ControlFlowGraph(null, "contract description", ControlFlowGraph.Kind.TopLevel), Mode.Body)

        return createContractDescriptionEnterNode().also {
            lastNodes.push(it)
            exitTargetsForTry.push(it)
        }
    }

    fun exitContractDescription() {
        lastNodes.pop()
        exitTargetsForTry.pop()
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

        call.acceptChildren(object : FirDefaultVisitor<Unit, Any?>() {
            override fun visitElement(element: FirElement, data: Any?) {
                element.acceptChildren(this, null)
            }

            override fun visitAnonymousFunction(anonymousFunction: FirAnonymousFunction, data: Any?) {
                anonymousFunction.controlFlowGraphReference?.accept(this, null)
            }

            override fun visitAnonymousObject(anonymousObject: FirAnonymousObject, data: Any?) {
                anonymousObject.controlFlowGraphReference?.accept(this, null)
            }

            override fun visitControlFlowGraphReference(controlFlowGraphReference: FirControlFlowGraphReference, data: Any?) {
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
        // If an expression, which returns Nothing, ...(1)
        val targetNode = when {
            tryExitNodes.isEmpty -> {
                // (1)... isn't inside a try expression, that is an uncaught exception path.
                exitTargetsForTry.top()
            }
            // (1)... inside a try expression...(2)
            finallyEnterNodes.topOrNull()?.level == levelCounter -> {
                // (2)... with finally
                // Either in try-main or catch. Route to `finally`
                finallyEnterNodes.top()
            }
            // (2)... without finally or within finally ...(3)
            tryExitNodes.top().fir.finallyBlock == null -> {
                // (3)... without finally ...(4)
                // Either in try-main or catch.
                val tryMainExitNode = tryMainExitNodes.top()
                if (tryMainExitNode.followingNodes.isNotEmpty()) {
                    // (4)... in catch, i.e., re-throw.
                    exitTargetsForTry.top()
                } else {
                    // (4)... in try-main. Route to exit of try main block.
                    // We already have edges from the exit of try main block to each catch clause.
                    // This edge makes the remaining part of try main block, e.g., following `when` branches, marked as dead.
                    tryMainExitNode
                }
            }
            // (3)... within finally.
            else -> exitTargetsForTry.top()
        }
        if (targetNode is TryMainBlockExitNode) {
            val catches = targetNode.fir.catches
            if (catches.isEmpty()) {
                addNodeWithJump(node, exitTargetsForTry.top(), preferredKind, label = UncaughtExceptionPath)
            } else {
                //edges to all the catches
                addNodeWithJumpToCatches(node, catches.map { catchNodeStorage[it]!! }, preferredKind = preferredKind)
            }
        } else {
            addNodeWithJump(node, targetNode, preferredKind, label = UncaughtExceptionPath)
        }
    }

    private fun addNodeWithJump(
        node: CFGNode<*>,
        targetNode: CFGNode<*>?,
        preferredKind: EdgeKind = EdgeKind.Forward,
        isBack: Boolean = false,
        label: EdgeLabel = NormalPath,
        labelForFinallyBLock: EdgeLabel = label,
        trackJump: Boolean = false
    ) {
        popAndAddEdge(node, preferredKind)
        if (targetNode != null) {
            if (isBack) {
                if (targetNode is LoopEnterNode) {
                    // `continue` to the loop header
                    addBackEdge(node, targetNode, label = LoopBackPath)
                } else {
                    addBackEdge(node, targetNode, label = label)
                }
            } else {
                // go through all final nodes between node and target
                val finallyNodes = finallyBefore(targetNode)
                val finalFrom = finallyNodes.fold(node) { from, (finallyEnter, tryExit) ->
                    addEdgeIfNotExist(from, finallyEnter, propagateDeadness = false, label = labelForFinallyBLock)
                    tryExit
                }
                addEdgeIfNotExist(
                    finalFrom,
                    targetNode,
                    propagateDeadness = false,
                    label = if (finallyNodes.isEmpty()) label else labelForFinallyBLock
                )
                if (trackJump && finallyNodes.isNotEmpty()) {
                    //actually we can store all returns like this, but not sure if it makes anything better
                    nonDirectJumps.put(targetNode, node)
                }
            }
        }
        val stub = createStubNode()
        addEdge(node, stub)
        lastNodes.push(stub)
    }

    private fun addNodeWithJumpToCatches(
        node: CFGNode<*>,
        targets: List<CatchClauseEnterNode>,
        label: EdgeLabel = UncaughtExceptionPath,
        preferredKind: EdgeKind = EdgeKind.Forward
    ) {
        require(targets.isNotEmpty())
        popAndAddEdge(node, preferredKind)
        targets.forEach { target ->
            addEdge(node, target, propagateDeadness = false, label = label)
        }
        val stub = createStubNode()
        addEdge(node, stub)
        lastNodes.push(stub)
    }

    private fun finallyBefore(target: CFGNode<*>): List<Pair<FinallyBlockEnterNode, FinallyBlockExitNode>> {
        return finallyEnterNodes.all().takeWhile { it.level > target.level }.map { finallyEnter ->
            val finallyExit = finallyExitNodes[finallyEnter.fir]!!
            finallyEnter to finallyExit
        }
    }

    private fun popAndAddEdge(to: CFGNode<*>, preferredKind: EdgeKind = EdgeKind.Forward) {
        addEdge(lastNodes.pop(), to, preferredKind = preferredKind)
    }

    private fun addEdgeIfNotExist(
        from: CFGNode<*>,
        to: CFGNode<*>,
        propagateDeadness: Boolean = true,
        isDead: Boolean = false,
        preferredKind: EdgeKind = EdgeKind.Forward,
        label: EdgeLabel = NormalPath
    ) {
        if (!from.followingNodes.contains(to)) {
            addEdge(from, to, propagateDeadness, isDead, preferredKind = preferredKind, label = label)
        }
    }

    private fun addEdge(
        from: CFGNode<*>,
        to: CFGNode<*>,
        propagateDeadness: Boolean = true,
        isDead: Boolean = false,
        isBack: Boolean = false,
        preferredKind: EdgeKind = EdgeKind.Forward,
        label: EdgeLabel = NormalPath
    ) {
        val kind = if (isDead || from.isDead || to.isDead) {
            if (isBack) EdgeKind.DeadBackward else EdgeKind.DeadForward
        } else preferredKind
        CFGNode.addEdge(from, to, kind, propagateDeadness, label)
    }

    private fun addBackEdge(
        from: CFGNode<*>,
        to: CFGNode<*>,
        isDead: Boolean = false,
        label: EdgeLabel = NormalPath
    ) {
        addEdge(from, to, propagateDeadness = false, isDead = isDead, isBack = true, preferredKind = EdgeKind.CfgBackward, label = label)
    }

    private fun propagateDeadnessForward(node: CFGNode<*>, deep: Boolean = false) {
        if (!node.isDead) return
        node.followingNodes
            .filter { node.outgoingEdges.getValue(it).kind == EdgeKind.Forward }
            .forEach { target ->
                CFGNode.addJustKindEdge(node, target, EdgeKind.DeadForward, false)
                target.updateDeadStatus()
                if (deep) {
                    propagateDeadnessForward(target, true)
                }
            }
    }

    // ----------------------------------- Utils -----------------------------------

    private inline fun Collection<FirDeclaration>.forEachMember(block: (FirDeclaration) -> Unit) {
        for (member in this) {
            for (callableDeclaration in member.unwrap()) {
                block(callableDeclaration)
            }
        }
    }

    private fun FirDeclaration.unwrap(): List<FirDeclaration> =
        when (this) {
            is FirFunction, is FirAnonymousInitializer, is FirField -> listOf(this)
            is FirProperty -> listOfNotNull(this.getter, this.setter, this)
            else -> emptyList()
        }

    private fun addNewSimpleNodeIfPossible(newNode: CFGNode<*>, isDead: Boolean = false): CFGNode<*>? {
        if (lastNodes.isEmpty) return null
        return addNewSimpleNode(newNode, isDead)
    }

    private fun <R> withLevelOfNode(node: CFGNode<*>, f: () -> R): R {
        val last = levelCounter
        levelCounter = node.level
        try {
            return f()
        } finally {
            levelCounter = last
        }
    }

}

fun FirDeclaration?.isLocalClassOrAnonymousObject() = ((this as? FirRegularClass)?.isLocal == true) || this is FirAnonymousObject
