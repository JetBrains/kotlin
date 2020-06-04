/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.dfa.cfg

import org.jetbrains.kotlin.contracts.description.InvocationKind
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
import kotlin.random.Random

class ControlFlowGraphBuilder {
    private val graphs: Stack<ControlFlowGraph> = stackOf(ControlFlowGraph(null, "<TOP_LEVEL_GRAPH>", ControlFlowGraph.Kind.TopLevel))
    val graph: ControlFlowGraph get() = graphs.top()

    private val lexicalScopes: Stack<Stack<CFGNode<*>>> = stackOf(stackOf())
    private val lastNodes: Stack<CFGNode<*>> get() = lexicalScopes.top()
    val lastNode: CFGNode<*> get() = lastNodes.top()

    private val exitNodes: Stack<CFGNode<*>> = stackOf()

    private val functionEnterNodes: SymbolBasedNodeStorage<FirFunction<*>, FunctionEnterNode> = SymbolBasedNodeStorage()
    private val functionExitNodes: SymbolBasedNodeStorage<FirFunction<*>, FunctionExitNode> = SymbolBasedNodeStorage()

    private val whenExitNodes: NodeStorage<FirWhenExpression, WhenExitNode> = NodeStorage()

    private val loopEnterNodes: NodeStorage<FirElement, CFGNode<FirElement>> = NodeStorage()
    private val loopExitNodes: NodeStorage<FirLoop, LoopExitNode> = NodeStorage()

    private val tryExitNodes: NodeStorage<FirTryExpression, TryExpressionExitNode> = NodeStorage()
    private val catchNodeStorages: Stack<NodeStorage<FirCatch, CatchClauseEnterNode>> = stackOf()
    private val catchNodeStorage: NodeStorage<FirCatch, CatchClauseEnterNode> get() = catchNodeStorages.top()

    private val binaryAndExitNodes: Stack<BinaryAndExitNode> = stackOf()
    private val binaryOrExitNodes: Stack<BinaryOrExitNode> = stackOf()

    private val topLevelVariableInitializerExitNodes: Stack<PropertyInitializerExitNode> = stackOf()

    private val initBlockExitNodes: Stack<InitBlockExitNode> = stackOf()

    private val exitSafeCallNodes: Stack<ExitSafeCallNode> = stackOf()

    private val blocksOfFunctions: MutableMap<FirBlock, FirFunction<*>> = mutableMapOf()
    private val exitsOfAnonymousFunctions: MutableMap<FirAnonymousFunction, FunctionExitNode> = mutableMapOf()

    private val entersToPostponedAnonymousFunctions: MutableMap<FirFunctionSymbol<*>, PostponedLambdaEnterNode> = mutableMapOf()
    private val exitsFromPostponedAnonymousFunctions: MutableMap<FirFunctionSymbol<*>, PostponedLambdaExitNode> = mutableMapOf()
    private val parentGraphForAnonymousFunctions: MutableMap<FirFunctionSymbol<*>, ControlFlowGraph> = mutableMapOf()

    private val exitsFromCompletedPostponedAnonymousFunctions: MutableList<PostponedLambdaExitNode> = mutableListOf()

    private val enterToLocalClassesMembers: MutableMap<AbstractFirBasedSymbol<*>, CFGNode<*>?> = mutableMapOf()

    var levelCounter: Int = 0
        private set

    private var idCounter: Int = Random.nextInt()
    private val shouldPassFlowFromInplaceLambda: Stack<Boolean> = stackOf(true)

    fun createId(): Int = idCounter++

    fun isTopLevel(): Boolean = graphs.size == 1

    // ----------------------------------- Named function -----------------------------------

    /*
     * Second argument of pair is not null only if function is local and it is a
     *   previous node before function declaration
     */
    fun enterFunction(function: FirFunction<*>): Pair<FunctionEnterNode, CFGNode<*>?> {
        val name = when (function) {
            is FirSimpleFunction -> function.name.asString()
            is FirPropertyAccessor -> if (function.isGetter) "<getter>" else "<setter>"
            is FirAnonymousFunction -> "<anonymous>" // TODO: add check to lambda or fun
            is FirConstructor -> "<init>"
            else -> throw IllegalArgumentException("Unknown function: ${function.render()}")
        }
        val invocationKind = function.invocationKind
        val isInplace = invocationKind.isInplace()

        val previousNode = entersToPostponedAnonymousFunctions[function.symbol]
            ?: enterToLocalClassesMembers[function.symbol]
            ?: if (!isInplace && function !is FirPropertyAccessor && graphs.topOrNull()?.let { it.kind == ControlFlowGraph.Kind.Function } == true) {
                lastNodes.top()
            } else {
                null
            }

        graphs.push(ControlFlowGraph(function, name, ControlFlowGraph.Kind.Function))

        val enterNode = createFunctionEnterNode(function).also {
            if (isInplace) {
                val postponedEnterNode = entersToPostponedAnonymousFunctions[function.symbol]
                if (postponedEnterNode != null) {
                    addEdge(postponedEnterNode, it)
                    lastNodes.push(it)
                } else {
                    addNewSimpleNode(it)
                }
            } else {
                lexicalScopes.push(stackOf())
                lastNodes.push(it)
            }
        }

        if (previousNode != null && !isInplace) {
            addEdge(previousNode, enterNode, preferredKind = EdgeKind.Dfg)
        }
        val exitNode = createFunctionExitNode(function)
        if (function is FirAnonymousFunction) {
            exitsOfAnonymousFunctions[function] = exitNode
        }

        @Suppress("NON_EXHAUSTIVE_WHEN")
        when (invocationKind) {
            InvocationKind.AT_MOST_ONCE, InvocationKind.UNKNOWN -> addEdge(enterNode, exitNode)
        }

        functionExitNodes.push(exitNode)
        if (!isInplace) {
            exitNodes.push(exitNode)
        } else {
            functionEnterNodes.push(enterNode)
        }
        levelCounter++
        return enterNode to previousNode
    }

    fun exitFunction(function: FirFunction<*>): Pair<FunctionExitNode, ControlFlowGraph> {
        levelCounter--
        val exitNode = functionExitNodes.pop()
        val invocationKind = function.invocationKind
        val isInplace = invocationKind != null
        if (!isInplace) {
            exitNodes.pop()
        }
        val postponedEnterNode = entersToPostponedAnonymousFunctions.remove(function.symbol)
        val postponedExitNode = exitsFromPostponedAnonymousFunctions.remove(function.symbol)
        if (isInplace) {
            val enterNode = functionEnterNodes.pop()
            @Suppress("NON_EXHAUSTIVE_WHEN")
            when (invocationKind) {
                InvocationKind.AT_LEAST_ONCE, InvocationKind.UNKNOWN -> addEdge(exitNode, enterNode, propagateDeadness = false)
            }
            if (postponedExitNode != null) {
                CFGNode.addEdge(lastNodes.pop(), postponedExitNode, propagateDeadness = true, kind = EdgeKind.Cfg)
                if (invocationKind == InvocationKind.EXACTLY_ONCE && shouldPassFlowFromInplaceLambda.top()) {
                    exitsFromCompletedPostponedAnonymousFunctions += postponedExitNode
                }
            }
        } else {
            if (function.body == null) {
                addEdge(lastNodes.pop(), exitNode)
            }
            if (postponedExitNode != null) {
                requireNotNull(postponedEnterNode)
                val kind = if (postponedEnterNode.isDead) EdgeKind.Dead else EdgeKind.Cfg
                CFGNode.addJustKindEdge(postponedEnterNode, postponedExitNode, kind, propagateDeadness = true)
            }
            lexicalScopes.pop()
        }
        exitNode.updateDeadStatus()
        val graph = graphs.pop().also { graph ->
            exitsFromCompletedPostponedAnonymousFunctions.removeAll { it.owner == graph }
        }
        val previousGraph = parentGraphForAnonymousFunctions.remove(function.symbol) ?: graphs.top()
        if (previousGraph.kind == ControlFlowGraph.Kind.Function) {
            previousGraph.addSubGraph(graph)
        }

        return exitNode to graph
    }

    fun returnExpressionsOfAnonymousFunction(function: FirAnonymousFunction): Collection<FirStatement> {
        fun FirElement.extractArgument(): FirElement = when {
            this is FirReturnExpression && target.labeledElement.symbol == function.symbol -> result.extractArgument()
            else -> this
        }

        fun CFGNode<*>.extractArgument(): FirElement? = when(this) {
            is FunctionEnterNode, is TryMainBlockEnterNode, is CatchClauseEnterNode -> null
            is ExitSafeCallNode -> lastPreviousNode.extractArgument()
            is StubNode -> firstPreviousNode.extractArgument()
            else -> fir.extractArgument()
        }

        val exitNode = function.controlFlowGraphReference.controlFlowGraph?.exitNode ?: exitsOfAnonymousFunctions.getValue(function)
        return exitNode.previousNodes.mapNotNullTo(mutableSetOf()) {
            it.extractArgument() as FirStatement?
        }
    }

    // ----------------------------------- Anonymous function -----------------------------------

    fun visitPostponedAnonymousFunction(anonymousFunction: FirAnonymousFunction): Pair<PostponedLambdaEnterNode, PostponedLambdaExitNode> {
        val enterNode = createPostponedLambdaEnterNode(anonymousFunction)
        val exitNode = createPostponedLambdaExitNode(anonymousFunction)
        val symbol = anonymousFunction.symbol
        entersToPostponedAnonymousFunctions[symbol] = enterNode
        exitsFromPostponedAnonymousFunctions[symbol] = exitNode
        parentGraphForAnonymousFunctions[symbol] = graph
        CFGNode.addEdge(lastNodes.pop(), enterNode, kind = EdgeKind.Simple, propagateDeadness = true)
        CFGNode.addEdge(enterNode, exitNode, kind = EdgeKind.Dfg, propagateDeadness = true)
        lastNodes.push(exitNode)
        return enterNode to exitNode
    }

    // ----------------------------------- Classes -----------------------------------

    fun enterClass() {
        levelCounter++
        graphs.push(ControlFlowGraph(null, "STUB_CLASS_GRAPH", ControlFlowGraph.Kind.ClassInitializer))
    }

    fun exitClass() {
        levelCounter--
        graphs.pop()
    }

    fun exitClass(klass: FirClass<*>): ControlFlowGraph {
        exitClass()
        val name = when (klass) {
            is FirAnonymousObject -> "<anonymous>"
            is FirRegularClass -> {
                klass.name.asString()
            }
            else -> throw IllegalArgumentException("Unknown class kind: ${klass::class}")
        }
        val classGraph = ControlFlowGraph(klass, name, ControlFlowGraph.Kind.ClassInitializer)
        graphs.push(classGraph)
        var node: CFGNode<*> = createClassEnterNode(klass)
        for (declaration in klass.declarations) {
            val graph = when (declaration) {
                is FirProperty -> declaration.controlFlowGraphReference.controlFlowGraph
                is FirAnonymousInitializer -> declaration.controlFlowGraphReference.controlFlowGraph
                else -> null
            } ?: continue
            addEdge(node, graph.enterNode, preferredKind = EdgeKind.Cfg)
            node = graph.exitNode
            classGraph.addSubGraph(graph)
        }
        val exitNode = createClassExitNode(klass)
        addEdge(node, exitNode, preferredKind = EdgeKind.Cfg)
        return graphs.pop()
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

    private inline fun Collection<FirSymbolOwner<*>>.forEachMember(block: (FirSymbolOwner<*>) -> Unit) {
        for (member in this) {
            for (callableDeclaration in member.unwrap()) {
                block(callableDeclaration)
            }
        }
    }

    private fun FirSymbolOwner<*>.unwrap(): List<FirSymbolOwner<*>> =
        when (this) {
            is FirFunction<*>,
            is FirAnonymousInitializer
            -> listOf(this)

            is FirProperty -> listOfNotNull(this.getter, this.setter, this)
            else -> emptyList()
        }

    fun exitLocalClass(klass: FirRegularClass): Pair<LocalClassExitNode, ControlFlowGraph> {
        val graph = exitClass(klass)
        val node = createLocalClassExitNode(klass).also {
            addNewSimpleNodeIfPossible(it)
        }
        return node to graph
    }

    fun exitAnonymousObject(anonymousObject: FirAnonymousObject): Pair<AnonymousObjectExitNode, ControlFlowGraph> {
        val graph = exitClass(anonymousObject)
        val node = createAnonymousObjectExitNode(anonymousObject).also {
            // Hack for initializers of enum entries
            addNewSimpleNodeIfPossible(it)
        }
        return node to graph
    }

    // ----------------------------------- Block -----------------------------------

    fun enterBlock(block: FirBlock): BlockEnterNode? {
        when (val lastNode = lastNode) {
            is FunctionEnterNode -> {
                blocksOfFunctions[block] = lastNode.fir
                return null
            }
            is DelegatedConstructorCallNode -> {
                val ownerEnterNode = lastNode.owner.enterNode
                if (ownerEnterNode is FunctionEnterNode) {
                    blocksOfFunctions[block] = ownerEnterNode.fir
                    return null
                }
            }
        }
        return createBlockEnterNode(block).also { addNewSimpleNode(it) }.also { levelCounter++ }
    }

    fun exitBlock(block: FirBlock): CFGNode<*> {
        val function = blocksOfFunctions.remove(block)
        return if (function != null) {
            functionExitNodes.top().also {
                addEdge(lastNodes.pop(), it)
                lastNodes.push(it)
                it.updateDeadStatus()
            }
        } else {
            levelCounter--
            createBlockExitNode(block).also { addNewSimpleNode(it) }
        }
    }

    // ----------------------------------- Property -----------------------------------

    fun enterProperty(property: FirProperty): PropertyInitializerEnterNode {
        graphs.push(ControlFlowGraph(property, "val ${property.name}", ControlFlowGraph.Kind.Function))
        val enterNode = createPropertyInitializerEnterNode(property)
        val exitNode = createPropertyInitializerExitNode(property)
        topLevelVariableInitializerExitNodes.push(exitNode)
        enterToLocalClassesMembers[property.symbol]?.let {
            addEdge(it, enterNode, preferredKind = EdgeKind.Dfg)
        }
        exitNodes.push(exitNode)
        lexicalScopes.push(stackOf(enterNode))
        graph.enterNode = enterNode
        graph.exitNode = exitNode
        levelCounter++
        return enterNode
    }

    fun exitProperty(property: FirProperty): Pair<PropertyInitializerExitNode, ControlFlowGraph> {
        val topLevelVariableExitNode = topLevelVariableInitializerExitNodes.pop().also {
            addNewSimpleNode(it)
            it.updateDeadStatus()
        }
        levelCounter--
        exitNodes.pop()
        lexicalScopes.pop()
        return topLevelVariableExitNode to graphs.pop()
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

    fun exitOperatorCall(operatorCall: FirOperatorCall): OperatorCallNode {
        return createOperatorCallNode(operatorCall).also { addNewSimpleNode(it) }
    }

    fun exitComparisonExpression(comparisonExpression: FirComparisonExpression): ComparisonExpressionNode {
        return ComparisonExpressionNode(graph, comparisonExpression, levelCounter, createId()).also { addNewSimpleNode(it) }
    }

    // ----------------------------------- Jump -----------------------------------

    fun exitJump(jump: FirJump<*>): JumpNode {
        val node = createJumpNode(jump)
        val nextNode = when (jump) {
            is FirReturnExpression -> functionExitNodes[jump.target.labeledElement.symbol]
            is FirContinueExpression -> loopEnterNodes[jump.target.labeledElement]
            is FirBreakExpression -> loopExitNodes[jump.target.labeledElement]
            else -> throw IllegalArgumentException("Unknown jump type: ${jump.render()}")
        }
        addNodeWithJump(node, nextNode)
        return node
    }

    // ----------------------------------- Check not null call -----------------------------------

    fun exitCheckNotNullCall(checkNotNullCall: FirCheckNotNullCall, callCompleted: Boolean): Pair<CheckNotNullCallNode, UnionFunctionCallArgumentsNode?> {
        val node = createCheckNotNullCallNode(checkNotNullCall).also { addNewSimpleNode(it) }
        val unionNode = processUnionOfArguments(node, callCompleted).second
        return node to unionNode
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
        addEdge(lastNodes.pop(), node)
        val whenExitNode = whenExitNodes.top()
        addEdge(node, whenExitNode, propagateDeadness = false)
        return node
    }

    fun exitWhenExpression(whenExpression: FirWhenExpression, callCompleted: Boolean): Triple<WhenExitNode, WhenSyntheticElseBranchNode?, UnionFunctionCallArgumentsNode?> {
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
        val (_, unionNode) = processUnionOfArguments(whenExitNode, callCompleted)
        levelCounter--
        return Triple(whenExitNode, syntheticElseBranchNode, unionNode)
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
        addEdge(lastNodes.pop(), loopBlockExitNode)
        if (lastNodes.isNotEmpty) {
            val conditionEnterNode = lastNodes.pop()
            require(conditionEnterNode is LoopConditionEnterNode) { loop.render() }
            addEdge(loopBlockExitNode, conditionEnterNode, propagateDeadness = false)
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
        addEdge(lastNodes.pop(), conditionExitNode)
        val blockEnterNode = lastNodes.pop()
        require(blockEnterNode is LoopBlockEnterNode)
        addEdge(conditionExitNode, blockEnterNode, propagateDeadness = false, isDead = conditionBooleanValue == false)
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

    private val finallyEnterNodes: Stack<FinallyBlockEnterNode> = stackOf()

    fun enterTryExpression(tryExpression: FirTryExpression): Pair<TryExpressionEnterNode, TryMainBlockEnterNode> {
        catchNodeStorages.push(NodeStorage())
        val enterTryExpressionNode = createTryExpressionEnterNode(tryExpression)
        addNewSimpleNode(enterTryExpressionNode)
        tryExitNodes.push(createTryExpressionExitNode(tryExpression))
        levelCounter++
        val enterTryNodeBlock = createTryMainBlockEnterNode(tryExpression)
        addNewSimpleNode(enterTryNodeBlock)
        addEdge(enterTryNodeBlock, exitNodes.top())

        for (catch in tryExpression.catches) {
            val catchNode = createCatchClauseEnterNode(catch)
            catchNodeStorage.push(catchNode)
            addEdge(enterTryNodeBlock, catchNode)
            addEdge(catchNode, exitNodes.top())
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
        addEdge(lastNodes.pop(), node)
        addEdge(node, tryExitNodes.top())
        return node
    }

    fun enterCatchClause(catch: FirCatch): CatchClauseEnterNode {
        return catchNodeStorage[catch]!!.also { lastNodes.push(it) }.also { levelCounter++ }
    }

    fun exitCatchClause(catch: FirCatch): CatchClauseExitNode {
        levelCounter--
        return createCatchClauseExitNode(catch).also {
            addEdge(lastNodes.pop(), it)
            addEdge(it, tryExitNodes.top(), propagateDeadness = false)
        }
    }

    fun enterFinallyBlock(tryExpression: FirTryExpression): FinallyBlockEnterNode {
        val enterNode = finallyEnterNodes.pop()
        lastNodes.push(enterNode)
        return enterNode
    }

    fun exitFinallyBlock(tryExpression: FirTryExpression): FinallyBlockExitNode {
        return createFinallyBlockExitNode(tryExpression).also {
            addEdge(lastNodes.pop(), it)
            addEdge(it, tryExitNodes.top())
        }
    }

    fun exitTryExpression(tryExpression: FirTryExpression, callCompleted: Boolean): Pair<TryExpressionExitNode, UnionFunctionCallArgumentsNode?> {
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

    fun enterCall(call: FirCall) {
        levelCounter++
    }

    fun exitFunctionCall(functionCall: FirFunctionCall, callCompleted: Boolean): Pair<FunctionCallNode, UnionFunctionCallArgumentsNode?> {
        levelCounter--
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

    fun exitDelegatedConstructorCall(call: FirDelegatedConstructorCall, callCompleted: Boolean): Pair<DelegatedConstructorCallNode, UnionFunctionCallArgumentsNode?> {
        levelCounter--
        val node = createDelegatedConstructorCallNode(call)
        val (kind, unionNode) = processUnionOfArguments(node, callCompleted)
        addNewSimpleNode(node, preferredKind = kind)
        return node to unionNode
    }

    private fun processUnionOfArguments(
        node: CFGNode<*>,
        callCompleted: Boolean
    ): Pair<EdgeKind, UnionFunctionCallArgumentsNode?> {
        if (!shouldPassFlowFromInplaceLambda.top()) return EdgeKind.Simple to null
        var kind = EdgeKind.Simple
        if (!callCompleted || exitsFromCompletedPostponedAnonymousFunctions.isEmpty()) {
            return EdgeKind.Simple to null
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
                addEdge(lastNodes.pop(), node, preferredKind = EdgeKind.Cfg)
                kind = EdgeKind.Dfg
                hasDirectPreviousNode = true
            }
            addEdge(exitNode.lastPreviousNode, unionNode, preferredKind = EdgeKind.Dfg)
            iterator.remove()
        }
        if (hasPostponedLambdas) {
            if (hasDirectPreviousNode) {
                lastNodes.push(unionNode)
            } else {
                addNewSimpleNode(unionNode)
            }
        } else {
            return EdgeKind.Simple to null
        }
        return Pair(kind, unionNode)
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

    // ----------------------------------- Annotations -----------------------------------

    fun enterAnnotationCall(annotationCall: FirAnnotationCall): AnnotationEnterNode {
        return createAnnotationEnterNode(annotationCall).also {
            if (graphs.top().kind == ControlFlowGraph.Kind.Function) {
                addNewSimpleNode(it)
            } else {
                lastNodes.push(it)
            }
        }
    }

    fun exitAnnotationCall(annotationCall: FirAnnotationCall): AnnotationExitNode {
        return createAnnotationExitNode(annotationCall).also {
            if (graphs.size > 1) {
                addNewSimpleNode(it)
            } else {
                lastNodes.pop()
            }
        }
    }

    // ----------------------------------- Block -----------------------------------

    fun enterInitBlock(initBlock: FirAnonymousInitializer): Pair<InitBlockEnterNode, CFGNode<*>?> {
        val lastNode = lastNodes.topOrNull()
        graphs.push(ControlFlowGraph(initBlock, "init block", ControlFlowGraph.Kind.Function))
        val enterNode = createInitBlockEnterNode(initBlock).also {
            lexicalScopes.push(stackOf(it))
        }
        val exitNode = createInitBlockExitNode(initBlock)
        enterToLocalClassesMembers[initBlock.symbol]?.let {
            addEdge(it, enterNode, preferredKind = EdgeKind.Dfg)
        }
        initBlockExitNodes.push(exitNode)
        exitNodes.push(exitNode)
        levelCounter++
        return enterNode to lastNode
    }

    fun exitInitBlock(initBlock: FirAnonymousInitializer): Pair<InitBlockExitNode, ControlFlowGraph> {
        levelCounter--
        val exitNode = initBlockExitNodes.pop().also {
            addNewSimpleNode(it)
            it.updateDeadStatus()
        }
        lexicalScopes.pop()
        exitNodes.pop()
        val graph = graphs.pop()
        return exitNode to graph
    }

    // ----------------------------------- Safe calls -----------------------------------

    fun enterSafeCall(safeCall: FirSafeCallExpression): EnterSafeCallNode {
        val lastNode = lastNodes.pop()
        val enterNode = createEnterSafeCallNode(safeCall)
        lastNodes.push(enterNode)
        val exitNode = createExitSafeCallNode(safeCall)
        exitSafeCallNodes.push(exitNode)
        addEdge(lastNode, enterNode)
        addEdge(lastNode, exitNode)
        return enterNode
    }

    fun exitSafeCall(safeCall: FirSafeCallExpression): ExitSafeCallNode {
        return exitSafeCallNodes.pop().also {
            addNewSimpleNode(it)
            it.updateDeadStatus()
        }
    }

    // ----------------------------------- Contract description -----------------------------------

    fun enterContractDescription(): CFGNode<*> {
        graphs.push(ControlFlowGraph(null, "contract description", ControlFlowGraph.Kind.TopLevel))
        val node = createContractDescriptionEnterNode().also {
            graph.enterNode = it
        }
        lexicalScopes.push(stackOf(node))
        return node
    }

    fun exitContractDescription() {
        lexicalScopes.pop()
        graphs.pop()
    }

    // -------------------------------------------------------------------------------------------------------------------------

    private fun addNodeThatReturnsNothing(node: CFGNode<*>, preferredKind: EdgeKind = EdgeKind.Simple) {
        /*
         * `return` is temporary solution that is needed for init block
         * it will be replaced after correct implementation of CFG for class initialization
         */
        val exitNode: CFGNode<*> = exitNodes.top()
        addNodeWithJump(node, exitNode, preferredKind)
    }

    private fun addNodeWithJump(node: CFGNode<*>, targetNode: CFGNode<*>?, preferredKind: EdgeKind = EdgeKind.Simple) {
        addEdge(lastNodes.pop(), node)
        if (targetNode != null) {
            addEdge(node, targetNode)
        }
        val stub = createStubNode()
        addEdge(node, stub)
        lastNodes.push(stub)
    }

    private fun addNewSimpleNode(newNode: CFGNode<*>, isDead: Boolean = false, preferredKind: EdgeKind = EdgeKind.Simple): CFGNode<*> {
        val oldNode = lastNodes.pop()
        addEdge(oldNode, newNode, isDead = isDead, preferredKind = preferredKind)
        lastNodes.push(newNode)
        return oldNode
    }

    private fun addNewSimpleNodeIfPossible(newNode: CFGNode<*>, isDead: Boolean = false): CFGNode<*>? {
        if (lastNodes.isEmpty) return null
        return addNewSimpleNode(newNode, isDead)
    }

    private fun addEdge(
        from: CFGNode<*>,
        to: CFGNode<*>,
        propagateDeadness: Boolean = true,
        isDead: Boolean = false,
        preferredKind: EdgeKind = EdgeKind.Simple
    ) {
        val kind = if (isDead || from.isDead || to.isDead) EdgeKind.Dead else preferredKind
        CFGNode.addEdge(from, to, kind, propagateDeadness)
    }

    private val FirFunction<*>.invocationKind: InvocationKind?
        get() = (this as? FirAnonymousFunction)?.invocationKind

    private fun InvocationKind?.isInplace(): Boolean {
        return this != null
    }

    fun reset() {
        exitsOfAnonymousFunctions.clear()
        exitsFromCompletedPostponedAnonymousFunctions.clear()
        lexicalScopes.reset()
        lexicalScopes.push(stackOf())
    }

    fun dropSubgraphFromCall(call: FirFunctionCall) {
        val graphs = mutableListOf<ControlFlowGraph>()

        call.acceptChildren(object : FirDefaultVisitorVoid() {
            override fun visitElement(element: FirElement) {
                element.acceptChildren(this)
            }

            override fun visitAnonymousFunction(anonymousFunction: FirAnonymousFunction) {
                anonymousFunction.controlFlowGraphReference.accept(this)
            }

            override fun visitAnonymousObject(anonymousObject: FirAnonymousObject) {
                anonymousObject.controlFlowGraphReference.accept(this)
            }

            override fun visitControlFlowGraphReference(controlFlowGraphReference: FirControlFlowGraphReference) {
                val graph = controlFlowGraphReference.controlFlowGraph ?: return
                if (graph.owner == null) return
                graphs += graph
            }
        }, null)

        val currentGraph = graph
        for (graph in graphs) {
            currentGraph.removeSubGraph(graph)
        }
    }
}
