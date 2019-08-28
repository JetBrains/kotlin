/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.dfa.cfg

import org.jetbrains.kotlin.contracts.description.InvocationKind
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.impl.FirAbstractPropertyAccessor
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.render
import org.jetbrains.kotlin.fir.resolve.dfa.*
import org.jetbrains.kotlin.fir.resolve.transformers.resultType
import org.jetbrains.kotlin.fir.types.isNothing

class ControlFlowGraphBuilder : ControlFlowGraphNodeBuilder() {
    private val graphs: Stack<ControlFlowGraph> = stackOf(ControlFlowGraph("<DUMP_GRAPH_FOR_ENUMS>"))
    override val graph: ControlFlowGraph get() = graphs.top()

    private val lexicalScopes: Stack<Stack<CFGNode<*>>> = stackOf(stackOf())
    private val lastNodes: Stack<CFGNode<*>> get() = lexicalScopes.top()
    val lastNode: CFGNode<*> get() = lastNodes.top()

    private val exitNodes: Stack<CFGNode<*>> = stackOf()

    private val functionExitNodes: NodeStorage<FirFunction<*>, FunctionExitNode> = NodeStorage(
        pushCallback = { exitNodes.push(it) },
        popCallback = { exitNodes.pop() }
    )

    private val whenExitNodes: NodeStorage<FirWhenExpression, WhenExitNode> = NodeStorage()

    private val loopEnterNodes: NodeStorage<FirElement, CFGNode<FirElement>> = NodeStorage()
    private val loopExitNodes: NodeStorage<FirLoop, LoopExitNode> = NodeStorage()

    private val tryExitNodes: NodeStorage<FirTryExpression, TryExpressionExitNode> = NodeStorage()
    private val catchNodeStorages: Stack<NodeStorage<FirCatch, CatchClauseEnterNode>> = stackOf()
    private val catchNodeStorage: NodeStorage<FirCatch, CatchClauseEnterNode> get() = catchNodeStorages.top()

    private val binaryAndExitNodes: Stack<BinaryAndExitNode> = stackOf()
    private val binaryOrExitNodes: Stack<BinaryOrExitNode> = stackOf()

    private val topLevelVariableExitNodes: Stack<PropertyExitNode> = stackWithCallbacks(
        pushCallback = { exitNodes.push(it) },
        popCallback = { exitNodes.pop() }
    )

    private val initBlockExitNodes: Stack<InitBlockExitNode> = stackWithCallbacks(
        pushCallback = { exitNodes.push(it) },
        popCallback = { exitNodes.pop() }
    )

    override var levelCounter: Int = 0

    fun isTopLevel(): Boolean = graphs.size == 1

    // ----------------------------------- Named function -----------------------------------

    fun enterFunction(function: FirFunction<*>): FunctionEnterNode {
        val name = when (function) {
            is FirNamedFunction -> function.name.asString()
            is FirAbstractPropertyAccessor -> if (function.isGetter) "<getter>" else "<setter>"
            is FirAnonymousFunction -> "<anonymous>" // TODO: add check to lambda or fun
            is FirConstructor -> function.name.asString()
            else -> throw IllegalArgumentException("Unknown function: ${function.render()}")
        }
        val invocationKind = function.invocationKind
        val isInplace = invocationKind.isInplace()
        if (!isInplace) {
            graphs.push(ControlFlowGraph(name))
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

        @Suppress("NON_EXHAUSTIVE_WHEN")
        when (invocationKind) {
            InvocationKind.AT_LEAST_ONCE -> addEdge(exitNode, enterNode)
            InvocationKind.AT_MOST_ONCE -> addEdge(enterNode, exitNode)
        }

        functionExitNodes.push(exitNode)
        levelCounter++
        return enterNode
    }

    fun exitFunction(function: FirFunction<*>): Pair<FunctionExitNode, ControlFlowGraph?> {
        levelCounter--
        val exitNode = functionExitNodes.pop()
        val isInplace = function.isInplace()
        if (isInplace) {
            addNewSimpleNode(exitNode)
        } else {
            addEdge(lastNodes.pop(), exitNode)
            lexicalScopes.pop()
        }
        val graph = if (!isInplace) {
            graphs.pop()
        } else {
            null
        }
        return exitNode to graph
    }

    // ----------------------------------- Block -----------------------------------

    fun enterBlock(block: FirBlock): BlockEnterNode {
        return createBlockEnterNode(block).also { addNewSimpleNode(it) }.also { levelCounter++ }
    }

    fun exitBlock(block: FirBlock): BlockExitNode {
        levelCounter--
        return createBlockExitNode(block).also { addNewSimpleNode(it) }
    }

    // ----------------------------------- Property -----------------------------------

    fun enterProperty(property: FirProperty): PropertyEnterNode {
        graphs.push(ControlFlowGraph("val ${property.name}"))
        val enterNode = createPropertyEnterNode(property)
        val exitNode = createPropertyExitNode(property)
        topLevelVariableExitNodes.push(exitNode)
        lexicalScopes.push(stackOf(enterNode))
        graph.enterNode = enterNode
        graph.exitNode = exitNode
        levelCounter++
        return enterNode
    }

    fun exitProperty(property: FirProperty): Pair<PropertyExitNode, ControlFlowGraph> {
        val topLevelVariableExitNode = topLevelVariableExitNodes.pop()
        addNewSimpleNode(topLevelVariableExitNode)
        levelCounter--
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
            is FirReturnExpression -> functionExitNodes[jump.target.labeledElement]
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

    fun exitWhenBranchCondition(whenBranch: FirWhenBranch): WhenBranchConditionExitNode {
        levelCounter--
        return createWhenBranchConditionExitNode(whenBranch).also {
            addNewSimpleNode(it)
            // put exit branch condition node twice so we can refer it after exit from when expression
            lastNodes.push(it)
        }.also { levelCounter++ }
    }

    fun exitWhenBranchResult(whenBranch: FirWhenBranch): WhenBranchResultExitNode {
        levelCounter--
        val node = createWhenBranchResultExitNode(whenBranch)
        addEdge(lastNodes.pop(), node)
        val whenExitNode = whenExitNodes.top()
        addEdge(node, whenExitNode, propagateDeadness = false)
        return node
    }

    fun exitWhenExpression(whenExpression: FirWhenExpression): WhenExitNode {
        levelCounter--
        // exit from last condition node still on stack
        // we should remove it
        require(lastNodes.pop() is WhenBranchConditionExitNode)
        val whenExitNode = whenExitNodes.pop()
        whenExitNode.markAsDeadIfNecessary()
        lastNodes.push(whenExitNode)
        return whenExitNode
    }

    // ----------------------------------- While Loop -----------------------------------

    fun enterWhileLoop(loop: FirLoop): LoopConditionEnterNode {
        addNewSimpleNode(createLoopEnterNode(loop))
        loopExitNodes.push(createLoopExitNode(loop))
        levelCounter++
        val node = createLoopConditionEnterNode(loop.condition)
        levelCounter++
        addNewSimpleNode(node)
        // put conditional node twice so we can refer it after exit from loop block
        lastNodes.push(node)
        loopEnterNodes.push(node)
        return node
    }

    fun exitWhileLoopCondition(loop: FirLoop): LoopConditionExitNode {
        levelCounter--
        val conditionExitNode = createLoopConditionExitNode(loop.condition)
        addNewSimpleNode(conditionExitNode)
        val conditionConstBooleanValue = conditionExitNode.booleanConstValue
        addEdge(conditionExitNode, loopExitNodes.top(), isDead = conditionConstBooleanValue == true)
        val loopBlockEnterNode = createLoopBlockEnterNode(loop)
        addNewSimpleNode(loopBlockEnterNode, conditionConstBooleanValue == false)
        levelCounter++
        return conditionExitNode
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

    fun enterDoWhileLoop(loop: FirLoop): LoopBlockEnterNode {
        addNewSimpleNode(createLoopEnterNode(loop))
        loopExitNodes.push(createLoopExitNode(loop))
        levelCounter++
        val blockEnterNode = createLoopBlockEnterNode(loop)
        addNewSimpleNode(blockEnterNode)
        // put block enter node twice so we can refer it after exit from loop condition
        lastNodes.push(blockEnterNode)
        loopEnterNodes.push(blockEnterNode)
        levelCounter++
        return blockEnterNode
    }

    fun enterDoWhileLoopCondition(loop: FirLoop): Pair<LoopBlockExitNode, LoopConditionEnterNode> {
        levelCounter--
        val blockExitNode = createLoopBlockExitNode(loop).also { addNewSimpleNode(it) }
        val conditionEnterNode = createLoopConditionEnterNode(loop.condition).also { addNewSimpleNode(it) }
        levelCounter++
        return blockExitNode to conditionEnterNode
    }

    fun exitDoWhileLoop(loop: FirLoop): LoopExitNode {
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
        return loopExit
    }

    // ----------------------------------- Boolean operators -----------------------------------

    fun enterBinaryAnd(binaryLogicExpression: FirBinaryLogicExpression): BinaryAndEnterNode {
        assert(binaryLogicExpression.kind == FirBinaryLogicExpression.OperationKind.AND)
        binaryAndExitNodes.push(createBinaryAndExitNode(binaryLogicExpression))
        return createBinaryAndEnterNode(binaryLogicExpression).also { addNewSimpleNode(it) }.also { levelCounter++ }
    }

    fun exitLeftBinaryAndArgument(binaryLogicExpression: FirBinaryLogicExpression): BinaryAndExitLeftOperandNode {
        assert(binaryLogicExpression.kind == FirBinaryLogicExpression.OperationKind.AND)
        val lastNode = lastNodes.pop()
        val leftBooleanConstValue = lastNode.booleanConstValue
        addEdge(lastNode, binaryAndExitNodes.top(), propagateDeadness = false, isDead = leftBooleanConstValue == true)
        return createBinaryAndExitLeftOperandNode(binaryLogicExpression).also {
            addEdge(lastNode, it, isDead = leftBooleanConstValue == false)
            lastNodes.push(it)
        }
    }

    fun exitBinaryAnd(binaryLogicExpression: FirBinaryLogicExpression): BinaryAndExitNode {
        levelCounter--
        assert(binaryLogicExpression.kind == FirBinaryLogicExpression.OperationKind.AND)
        return binaryAndExitNodes.pop().also {
            val rightNode = lastNodes.pop()
            addEdge(rightNode, it, propagateDeadness = false, isDead = it.leftOperandNode.booleanConstValue == false)
            it.markAsDeadIfNecessary()
            lastNodes.push(it)
        }
    }

    fun enterBinaryOr(binaryLogicExpression: FirBinaryLogicExpression): BinaryOrEnterNode {
        assert(binaryLogicExpression.kind == FirBinaryLogicExpression.OperationKind.OR)
        binaryOrExitNodes.push(createBinaryOrExitNode(binaryLogicExpression))
        return createBinaryOrEnterNode(binaryLogicExpression).also {
            addNewSimpleNode(it)
        }.also { levelCounter++ }
    }

    fun exitLeftBinaryOrArgument(binaryLogicExpression: FirBinaryLogicExpression): BinaryOrExitLeftOperandNode {
        levelCounter--
        assert(binaryLogicExpression.kind == FirBinaryLogicExpression.OperationKind.OR)
        val previousNode = lastNodes.pop()
        val leftBooleanValue = previousNode.booleanConstValue
        addEdge(previousNode, binaryOrExitNodes.top(), isDead = leftBooleanValue == false)
        return createBinaryOrExitLeftOperandNode(binaryLogicExpression).also {
            addEdge(previousNode, it, isDead = leftBooleanValue == true)
            lastNodes.push(it)
            levelCounter++
        }
    }

    fun exitBinaryOr(binaryLogicExpression: FirBinaryLogicExpression): BinaryOrExitNode {
        assert(binaryLogicExpression.kind == FirBinaryLogicExpression.OperationKind.OR)
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

    fun enterTryExpression(tryExpression: FirTryExpression): TryMainBlockEnterNode {
        catchNodeStorages.push(NodeStorage())
        addNewSimpleNode(createTryExpressionEnterNode(tryExpression))
        tryExitNodes.push(createTryExpressionExitNode(tryExpression))
        levelCounter++
        val tryNode = createTryMainBlockEnterNode(tryExpression)
        addNewSimpleNode(tryNode)
        addEdge(tryNode, exitNodes.top())

        for (catch in tryExpression.catches) {
            val catchNode = createCatchClauseEnterNode(catch)
            catchNodeStorage.push(catchNode)
            addEdge(tryNode, catchNode)
            addEdge(catchNode, exitNodes.top())
        }
        levelCounter++

        if (tryExpression.finallyBlock != null) {
            val finallyEnterNode = createFinallyBlockEnterNode(tryExpression)
            addEdge(tryNode, finallyEnterNode)
            finallyEnterNodes.push(finallyEnterNode)
        }

        return tryNode
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
        val node = createQualifiedAccessNode(qualifiedAccessExpression, returnsNothing)
        if (returnsNothing) {
            addNodeThatReturnsNothing(node)
        } else {
            addNewSimpleNode(node)
        }
        return node
    }

    fun exitFunctionCall(functionCall: FirFunctionCall): FunctionCallNode {
        val returnsNothing = functionCall.resultType.isNothing
        val node = createFunctionCallNode(functionCall, returnsNothing)
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

    fun exitVariableDeclaration(variable: FirVariable<*>): VariableDeclarationNode {
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
        val enterNode = createInitBlockEnterNode(initBlock).also {
            lexicalScopes.push(stackOf(it))
        }
        val exitNode = createInitBlockExitNode(initBlock)
        initBlockExitNodes.push(exitNode)
        levelCounter++
        return enterNode
    }

    fun exitInitBlock(initBlock: FirAnonymousInitializer): InitBlockExitNode {
        levelCounter--
        return initBlockExitNodes.pop().also {
            addNewSimpleNode(it)
            lexicalScopes.pop()
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
        return this != null && this != InvocationKind.UNKNOWN
    }

    private fun FirFunction<*>.isInplace(): Boolean {
        val invocationKind = this.invocationKind
        return invocationKind != null && invocationKind != InvocationKind.UNKNOWN
    }
}