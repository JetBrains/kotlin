/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.dfa.cfg

import org.jetbrains.kotlin.contracts.description.InvocationKind
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.render
import org.jetbrains.kotlin.fir.resolve.dfa.*
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.resultType
import org.jetbrains.kotlin.fir.types.isNothing

class ControlFlowGraphBuilder {
    private val graphs: Stack<ControlFlowGraph> = stackOf(ControlFlowGraph("<DUMP_GRAPH_FOR_ENUMS>", ControlFlowGraph.Kind.TopLevel))
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

    var levelCounter: Int = 0
        private set

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
            is FirConstructor -> function.name.asString()
            else -> throw IllegalArgumentException("Unknown function: ${function.render()}")
        }
        val invocationKind = function.invocationKind
        val isInplace = invocationKind.isInplace()

        val previousNode = if (!isInplace && graphs.topOrNull()?.let { it.kind != ControlFlowGraph.Kind.TopLevel } == true) {
            lastNodes.top()
        } else {
            null
        }

        if (!isInplace) {
            graphs.push(ControlFlowGraph(name, ControlFlowGraph.Kind.Function))
        }

        val enterNode = createFunctionEnterNode(function, isInplace).also {
            if (isInplace) {
                addNewSimpleNode(it)
            } else {
                lexicalScopes.push(stackOf())
                lastNodes.push(it)
            }
        }
        val exitNode = createFunctionExitNode(function, isInplace)
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

    fun exitFunction(function: FirFunction<*>): Pair<FunctionExitNode, ControlFlowGraph?> {
        levelCounter--
        val exitNode = functionExitNodes.pop()
        val invocationKind = function.invocationKind
        val isInplace = invocationKind != null
        if (!isInplace) {
            exitNodes.pop()
        }
        if (isInplace) {
            val enterNode = functionEnterNodes.pop()
            when (invocationKind) {
                InvocationKind.AT_LEAST_ONCE, InvocationKind.UNKNOWN -> addEdge(exitNode, enterNode, propagateDeadness = false)
            }
        } else {
            if (function.body == null) {
                addEdge(lastNodes.pop(), exitNode)
            }
            lexicalScopes.pop()
        }
        exitNode.markAsDeadIfNecessary()
        val graph = if (!isInplace) {
            graphs.pop()
        } else {
            null
        }
        return exitNode to graph
    }

    fun returnExpressionsOfAnonymousFunction(function: FirAnonymousFunction): List<FirStatement> {
        fun FirElement.extractArgument(): FirElement = when {
            this is FirReturnExpression && target.labeledElement.symbol == function.symbol -> result.extractArgument()
            else -> this
        }

        fun CFGNode<*>.extractArgument(): FirElement? = when(this) {
            is FunctionEnterNode, is TryMainBlockEnterNode, is CatchClauseEnterNode -> null
            is ExitSafeCallNode -> previousNodes.last().extractArgument()
            is StubNode -> previousNodes.first().extractArgument()
            else -> fir.extractArgument()
        }
        return exitsOfAnonymousFunctions.getValue(function).previousNodes.mapNotNull {
            it.extractArgument() as FirStatement?
        }
    }

    // ----------------------------------- Block -----------------------------------

    fun enterBlock(block: FirBlock): BlockEnterNode? {
        val lastNode = lastNode
        return if (lastNode is FunctionEnterNode) {
            blocksOfFunctions[block] = lastNode.fir
            null
        } else {
            createBlockEnterNode(block).also { addNewSimpleNode(it) }.also { levelCounter++ }
        }
    }

    fun exitBlock(block: FirBlock): CFGNode<*> {
        val function = blocksOfFunctions.remove(block)
        return if (function != null) {
            functionExitNodes.top().also {
                addEdge(lastNodes.pop(), it)
                lastNodes.push(it)
                it.markAsDeadIfNecessary()
            }
        } else {
            levelCounter--
            createBlockExitNode(block).also { addNewSimpleNode(it) }
        }
    }

    // ----------------------------------- Property -----------------------------------

    fun enterProperty(property: FirProperty): PropertyInitializerEnterNode {
        graphs.push(ControlFlowGraph("val ${property.name}", ControlFlowGraph.Kind.PropertyInitializer))
        val enterNode = createPropertyInitializerEnterNode(property)
        val exitNode = createPropertyInitializerExitNode(property)
        topLevelVariableInitializerExitNodes.push(exitNode)
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
            it.markAsDeadIfNecessary()
        }
        levelCounter--
        exitNodes.pop()
        lexicalScopes.pop()
        return topLevelVariableExitNode to graphs.pop()
    }

    // ----------------------------------- Operator call -----------------------------------

    fun exitTypeOperatorCall(typeOperatorCall: FirTypeOperatorCall): TypeOperatorCallNode {
        return createTypeOperatorCallNode(typeOperatorCall).also { addNewSimpleNode(it) }
    }

    fun exitOperatorCall(operatorCall: FirOperatorCall): OperatorCallNode {
        return createOperatorCallNode(operatorCall).also { addNewSimpleNode(it) }
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
        whenExitNode.markAsDeadIfNecessary()
        lastNodes.push(whenExitNode)
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
        addEdge(conditionExitNode, loopExitNodes.top(), isDead = conditionConstBooleanValue == true)
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
        loopExitNode.markAsDeadIfNecessary()
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
        loopExit.markAsDeadIfNecessary()
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
            it.markAsDeadIfNecessary()
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
            addEdge(it, binaryOrExitNodes.top(), isDead = leftBooleanValue == false)
        }

        val rightExitNode = createBinaryOrEnterRightOperandNode(binaryLogicExpression).also {
            addEdge(leftExitNode, it, isDead = leftBooleanValue == true)
            lastNodes.push(it)
            levelCounter++
        }
        return leftExitNode to rightExitNode
    }

    fun enterContract(functionCall: FirFunctionCall): EnterContractNode {
        return createEnterContractNode(functionCall).also { addNewSimpleNode(it) }
    }

    fun exitContract(functionCall: FirFunctionCall): ExitContractNode {
        return createExitContractNode(functionCall).also { addNewSimpleNode(it) }
    }

    fun exitBinaryOr(binaryLogicExpression: FirBinaryLogicExpression): BinaryOrExitNode {
        assert(binaryLogicExpression.kind == LogicOperationKind.OR)
        levelCounter--
        return binaryOrExitNodes.pop().also {
            val rightNode = lastNodes.pop()
            addEdge(rightNode, it, propagateDeadness = false)
            it.markAsDeadIfNecessary()
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

    fun exitTryExpression(tryExpression: FirTryExpression): TryExpressionExitNode {
        levelCounter--
        catchNodeStorages.pop()
        val node = tryExitNodes.pop()
        node.markAsDeadIfNecessary()
        lastNodes.push(node)
        return node
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

    fun exitFunctionCall(functionCall: FirFunctionCall): FunctionCallNode {
        val returnsNothing = functionCall.resultType.isNothing
        val node = createFunctionCallNode(functionCall)
        if (returnsNothing) {
            addNodeThatReturnsNothing(node)
        } else {
            addNewSimpleNode(node)
        }
        return node
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
            if (graphs.size > 1) {
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

    fun enterInitBlock(initBlock: FirAnonymousInitializer): InitBlockEnterNode {
        graphs.push(ControlFlowGraph("init block", ControlFlowGraph.Kind.ClassInitializer))
        val enterNode = createInitBlockEnterNode(initBlock).also {
            lexicalScopes.push(stackOf(it))
        }
        val exitNode = createInitBlockExitNode(initBlock)
        initBlockExitNodes.push(exitNode)
        exitNodes.push(exitNode)
        levelCounter++
        return enterNode
    }

    fun exitInitBlock(initBlock: FirAnonymousInitializer): InitBlockExitNode {
        levelCounter--
        return initBlockExitNodes.pop().also {
            addNewSimpleNode(it)
            it.markAsDeadIfNecessary()
            lexicalScopes.pop()
            exitNodes.pop()
            graphs.pop()
        }
    }

    // ----------------------------------- Safe calls -----------------------------------

    fun enterSafeCall(qualifiedAccess: FirQualifiedAccess): EnterSafeCallNode {
        val lastNode = lastNodes.pop()
        val enterNode = createEnterSafeCallNode(qualifiedAccess)
        lastNodes.push(enterNode)
        val exitNode = createExitSafeCallNode(qualifiedAccess)
        exitSafeCallNodes.push(exitNode)
        addEdge(lastNode, enterNode)
        addEdge(lastNode, exitNode)
        return enterNode
    }

    fun exitSafeCall(qualifiedAccess: FirQualifiedAccess): ExitSafeCallNode {
        return exitSafeCallNodes.pop().also {
            addNewSimpleNode(it)
        }
    }

    // -------------------------------------------------------------------------------------------------------------------------

    private fun CFGNode<*>.markAsDeadIfNecessary() {
        isDead = previousNodes.all { it.isDead }
    }

    private fun addNodeThatReturnsNothing(node: CFGNode<*>) {
        /*
         * `return` is temporary solution that is needed for init block
         * it will be replaced after correct implementation of CFG for class initialization
         */
        val exitNode: CFGNode<*> = exitNodes.top()
        addNodeWithJump(node, exitNode)
    }

    private fun addNodeWithJump(node: CFGNode<*>, targetNode: CFGNode<*>?) {
        addEdge(lastNodes.pop(), node)
        if (targetNode != null) {
            addEdge(node, targetNode)
        }
        val stub = createStubNode()
        addEdge(node, stub)
        lastNodes.push(stub)
    }

    private fun addNewSimpleNode(newNode: CFGNode<*>, isDead: Boolean = false): CFGNode<*> {
        val oldNode = lastNodes.pop()
        addEdge(oldNode, newNode, isDead = isDead)
        lastNodes.push(newNode)
        return oldNode
    }

    private fun addDeadEdge(from: CFGNode<*>, to: CFGNode<*>, propagateDeadness: Boolean) {
        val stub = createStubNode()
        addEdge(from, stub)
        addEdge(stub, to, propagateDeadness = propagateDeadness)
    }

    private fun addEdge(from: CFGNode<*>, to: CFGNode<*>, propagateDeadness: Boolean = true, isDead: Boolean = false) {
        if (isDead) {
            addDeadEdge(from, to, propagateDeadness)
            return
        }
        if (propagateDeadness && from.isDead) {
            to.isDead = true
        }
        from.followingNodes += to
        to.previousNodes += from
    }

    private val FirFunction<*>.invocationKind: InvocationKind?
        get() = (this as? FirAnonymousFunction)?.invocationKind

    private fun InvocationKind?.isInplace(): Boolean {
        return this != null
    }

    fun reset() {
        exitsOfAnonymousFunctions.clear()
    }
}