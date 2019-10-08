/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.dfa

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirResolvedCallableReference
import org.jetbrains.kotlin.fir.declarations.FirAnonymousInitializer
import org.jetbrains.kotlin.fir.declarations.FirFunction
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.impl.FirThisReceiverExpressionImpl
import org.jetbrains.kotlin.fir.resolve.BodyResolveComponents
import org.jetbrains.kotlin.fir.resolve.ImplicitReceiverStackImpl
import org.jetbrains.kotlin.fir.resolve.dfa.Condition.*
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.*
import org.jetbrains.kotlin.fir.resolve.transformers.FirBodyResolveTransformer
import org.jetbrains.kotlin.fir.symbols.AbstractFirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.CallableId
import org.jetbrains.kotlin.fir.symbols.FirSymbolOwner
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirVariableSymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.coneTypeSafe
import org.jetbrains.kotlin.fir.types.coneTypeUnsafe
import org.jetbrains.kotlin.fir.types.isMarkedNullable
import org.jetbrains.kotlin.ir.expressions.IrConstKind
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

class FirDataFlowAnalyzer(transformer: FirBodyResolveTransformer) : BodyResolveComponents by transformer {
    companion object {
        private val KOTLIN_BOOLEAN_NOT = CallableId(FqName("kotlin"), FqName("Boolean"), Name.identifier("not"))
    }

    private val context: DataFlowInferenceContext get() = inferenceComponents.ctx as DataFlowInferenceContext
    private val receiverStack: ImplicitReceiverStackImpl = transformer.implicitReceiverStack

    private val graphBuilder = ControlFlowGraphBuilder()
    private val logicSystem: LogicSystem = LogicSystemImpl(context)
    private val variableStorage = DataFlowVariableStorage()
    private val flowOnNodes = mutableMapOf<CFGNode<*>, Flow>()

    private val variablesForWhenConditions = mutableMapOf<WhenBranchConditionExitNode, DataFlowVariable>()

    /*
     * If there is no types from smartcasts function returns null
     *
     * Note that return value based on state of DataFlowAnalyzer (despite of stateless model of old frontend)
     */
    fun getTypeUsingSmartcastInfo(qualifiedAccessExpression: FirQualifiedAccessExpression): Collection<ConeKotlinType>? {
        /*
         * DataFlowAnalyzer holds variables only for declarations that have some smartcast (or can have)
         * If there is no useful information there is no data flow variable also
         */
        val variable = qualifiedAccessExpression.realVariable?.variableUnderAlias ?: return null
        return graphBuilder.lastNode.flow.getApprovedInfo(variable)?.exactType ?: return null
    }

    // ----------------------------------- Named function -----------------------------------

    fun enterFunction(function: FirFunction<*>) {
        val (functionEnterNode, previousNode) = graphBuilder.enterFunction(function)
        if (previousNode == null) {
            functionEnterNode.mergeIncomingFlow()
        } else {
            assert(functionEnterNode.previousNodes.isEmpty())
            functionEnterNode.flow = logicSystem.forkFlow(previousNode.flow)
        }
    }

    fun exitFunction(function: FirFunction<*>): ControlFlowGraph? {
        val (node, graph) = graphBuilder.exitFunction(function)
        node.mergeIncomingFlow()
        for (valueParameter in function.valueParameters) {
            variableStorage.removeRealVariable(valueParameter.symbol)
        }
        if (graphBuilder.isTopLevel()) {
            flowOnNodes.clear()
            variableStorage.reset()
        }
        return graph
    }

    // ----------------------------------- Property -----------------------------------

    fun enterProperty(property: FirProperty) {
        graphBuilder.enterProperty(property).mergeIncomingFlow()
    }

    fun exitProperty(property: FirProperty): ControlFlowGraph {
        val (node, graph) = graphBuilder.exitProperty(property)
        node.mergeIncomingFlow()
        return graph
    }

    // ----------------------------------- Block -----------------------------------

    fun enterBlock(block: FirBlock) {
        graphBuilder.enterBlock(block).mergeIncomingFlow()
    }

    fun exitBlock(block: FirBlock) {
        graphBuilder.exitBlock(block).mergeIncomingFlow()
    }

    // ----------------------------------- Operator call -----------------------------------

    fun exitTypeOperatorCall(typeOperatorCall: FirTypeOperatorCall) {
        val node = graphBuilder.exitTypeOperatorCall(typeOperatorCall).mergeIncomingFlow()

        if (typeOperatorCall.operation !in FirOperation.TYPES) return
        val type = typeOperatorCall.conversionTypeRef.coneTypeSafe<ConeKotlinType>() ?: return
        val operandVariable = getOrCreateRealVariable(typeOperatorCall.argument)?.variableUnderAlias ?: return

        val flow = node.flow
        when (typeOperatorCall.operation) {
            FirOperation.IS, FirOperation.NOT_IS -> {
                val expressionVariable = getOrCreateSyntheticVariable(typeOperatorCall)

                val trueInfo = FirDataFlowInfo(setOf(type), emptySet())
                val falseInfo = FirDataFlowInfo(emptySet(), setOf(type))

                fun chooseInfo(trueBranch: Boolean) =
                    if ((typeOperatorCall.operation == FirOperation.IS) == trueBranch) trueInfo else falseInfo

                logicSystem.addConditionalInfo(
                    flow,
                    expressionVariable,
                    ConditionalFirDataFlowInfo(
                        EqTrue, operandVariable, chooseInfo(true)
                    )
                )

                logicSystem.addConditionalInfo(
                    flow,
                    expressionVariable,
                    ConditionalFirDataFlowInfo(
                        EqFalse, operandVariable, chooseInfo(false)
                    )
                )
            }

            FirOperation.AS -> {
                logicSystem.addApprovedInfo(flow, operandVariable, FirDataFlowInfo(setOf(type), emptySet()))
            }

            FirOperation.SAFE_AS -> {
                val expressionVariable = getOrCreateSyntheticVariable(typeOperatorCall)
                logicSystem.addConditionalInfo(
                    flow,
                    expressionVariable,
                    ConditionalFirDataFlowInfo(
                        NotEqNull, operandVariable, FirDataFlowInfo(setOf(type), emptySet())
                    )
                )
                logicSystem.addConditionalInfo(
                    flow,
                    expressionVariable,
                    ConditionalFirDataFlowInfo(
                        EqNull, operandVariable, FirDataFlowInfo(emptySet(), setOf(type))
                    )
                )
            }

            else -> throw IllegalStateException()
        }

        node.flow = flow
    }

    fun exitOperatorCall(operatorCall: FirOperatorCall) {
        val node = graphBuilder.exitOperatorCall(operatorCall).mergeIncomingFlow()
        when (val operation = operatorCall.operation) {
            FirOperation.EQ, FirOperation.NOT_EQ, FirOperation.IDENTITY, FirOperation.NOT_IDENTITY -> {
                val leftOperand = operatorCall.arguments[0]
                val rightOperand = operatorCall.arguments[1]

                val leftConst = leftOperand as? FirConstExpression<*>
                val rightConst = rightOperand as? FirConstExpression<*>

                when {
                    leftConst?.kind == IrConstKind.Null -> processEqNull(node, rightOperand, operation)
                    rightConst?.kind == IrConstKind.Null -> processEqNull(node, leftOperand, operation)
                    leftConst != null -> processEqWithConst(node, rightOperand, leftConst, operation)
                    rightConst != null -> processEqWithConst(node, leftOperand, rightConst, operation)
                    operation != FirOperation.EQ && operation != FirOperation.IDENTITY -> return
                    else -> processEq(node, leftOperand, rightOperand, operation)
                }
            }
        }
    }

    private fun processEqWithConst(node: OperatorCallNode, operand: FirExpression, const: FirConstExpression<*>, operation: FirOperation) {
        val isEq = when (operation) {
            FirOperation.EQ, FirOperation.IDENTITY -> true
            FirOperation.NOT_EQ, FirOperation.NOT_IDENTITY -> false
            else -> return
        }

        val expressionVariable = getOrCreateVariable(node.fir)
        val flow = node.flow

        // not null for comparisons with constants
        getRealVariablesForSafeCallChain(operand).takeIf { it.isNotEmpty() }?.let { operandVariables ->
            operandVariables.forEach { operandVariable ->
                logicSystem.addConditionalInfo(
                    flow,
                    expressionVariable, ConditionalFirDataFlowInfo(
                        isEq.toEqBoolean(),
                        operandVariable,
                        FirDataFlowInfo(setOf(session.builtinTypes.anyType.coneTypeUnsafe()), emptySet())
                    )
                )
            }
        }

        // propagating facts for (... == true) and (... == false)
        variableStorage[operand]?.let { operandVariable ->
            if (const.kind != IrConstKind.Boolean) return@let

            val constValue = (const.value as Boolean)
            val shouldInvert = isEq xor constValue

            flow.getConditionalInfos(operandVariable).forEach { info ->
                logicSystem.addConditionalInfo(flow, expressionVariable, info.let { if (shouldInvert) it.invert() else it })
            }
        }
    }

    private fun processEq(node: OperatorCallNode, leftOperand: FirExpression, rightOperand: FirExpression, operation: FirOperation) {
        val leftType = leftOperand.typeRef.coneTypeSafe<ConeKotlinType>() ?: return
        val rightType = rightOperand.typeRef.coneTypeSafe<ConeKotlinType>() ?: return
        when {
            leftType.isMarkedNullable && rightType.isMarkedNullable -> return
            leftType.isMarkedNullable -> processEqNull(node, leftOperand, FirOperation.NOT_EQ)
            rightType.isMarkedNullable -> processEqNull(node, rightOperand, FirOperation.NOT_EQ)
        }
        // TODO: process EQUALITY
    }

    private fun processEqNull(node: OperatorCallNode, operand: FirExpression, operation: FirOperation) {
        val flow = node.flow
        val expressionVariable = getOrCreateVariable(node.fir)

        variableStorage[operand]?.let { operandVariable ->
            val condition = when (operation) {
                FirOperation.EQ, FirOperation.IDENTITY -> EqNull
                FirOperation.NOT_EQ, FirOperation.NOT_IDENTITY -> NotEqNull
                else -> throw IllegalArgumentException()
            }
            val facts = logicSystem.approveFact(operandVariable, condition, flow)
            facts.forEach { (variable, info) ->
                logicSystem.addConditionalInfo(
                    flow,
                    expressionVariable,
                    ConditionalFirDataFlowInfo(
                        EqTrue, variable, info
                    )
                )
                logicSystem.addConditionalInfo(
                    flow,
                    expressionVariable,
                    ConditionalFirDataFlowInfo(
                        EqFalse, variable, info.invert()
                    )
                )
            }
            node.flow = flow
            return
        }

        val operandVariables = getRealVariablesForSafeCallChain(operand).takeIf { it.isNotEmpty() } ?: return

        val condition = when (operation) {
            FirOperation.EQ, FirOperation.IDENTITY -> EqFalse
            FirOperation.NOT_EQ, FirOperation.NOT_IDENTITY -> EqTrue
            else -> throw IllegalArgumentException()
        }

        operandVariables.forEach { operandVariable ->
            logicSystem.addConditionalInfo(
                flow,
                expressionVariable, ConditionalFirDataFlowInfo(
                    condition,
                    operandVariable,
                    FirDataFlowInfo(setOf(session.builtinTypes.anyType.coneTypeUnsafe()), emptySet())
                )
            )
            // TODO: design do we need casts to Nothing?
            /*
            flow = addConditionalInfo(
                expressionVariable, UnapprovedFirDataFlowInfo(
                    eq(conditionValue.invert()!!),
                    operandVariable,
                    FirDataFlowInfo(setOf(session.builtinTypes.nullableNothingType.coneTypeUnsafe()), emptySet())
                )
            )
            */

        }
    }

    // ----------------------------------- Jump -----------------------------------

    fun exitJump(jump: FirJump<*>) {
        graphBuilder.exitJump(jump).mergeIncomingFlow()
    }

    // ----------------------------------- When -----------------------------------

    fun enterWhenExpression(whenExpression: FirWhenExpression) {
        graphBuilder.enterWhenExpression(whenExpression).mergeIncomingFlow()
    }

    fun enterWhenBranchCondition(whenBranch: FirWhenBranch) {
        val node = graphBuilder.enterWhenBranchCondition(whenBranch).mergeIncomingFlow()
        val previousNode = node.previousNodes.single()
        if (previousNode is WhenBranchConditionExitNode) {
            node.flow = logicSystem.approveFactsInsideFlow(
                variablesForWhenConditions.remove(previousNode)!!,
                EqFalse,
                node.flow,
                shouldForkFlow = true,
                shouldRemoveSynthetics = true
            )
        }
    }

    fun exitWhenBranchCondition(whenBranch: FirWhenBranch) {
        val (conditionExitNode, branchEnterNode) = graphBuilder.exitWhenBranchCondition(whenBranch)
        conditionExitNode.mergeIncomingFlow()

        val conditionVariable = getOrCreateVariable(whenBranch.condition)
        variablesForWhenConditions[conditionExitNode] = conditionVariable
        branchEnterNode.flow = logicSystem.approveFactsInsideFlow(
            conditionVariable,
            EqTrue,
            conditionExitNode.flow,
            shouldForkFlow = true,
            shouldRemoveSynthetics = false
        )
    }

    fun exitWhenBranchResult(whenBranch: FirWhenBranch) {
        graphBuilder.exitWhenBranchResult(whenBranch).mergeIncomingFlow()
    }

    fun exitWhenExpression(whenExpression: FirWhenExpression) {
        val node = graphBuilder.exitWhenExpression(whenExpression)
        val previousFlows = node.alivePreviousNodes.map { it.flow }
        val flow = logicSystem.joinFlow(previousFlows)
        node.flow = flow
        // TODO
        // val subjectSymbol = whenExpression.subjectVariable?.symbol
        // if (subjectSymbol != null) {
        //     variableStorage[subjectSymbol]?.let { flow = flow.removeVariable(it) }
        // }
        // node.flow = flow
    }

    // ----------------------------------- While Loop -----------------------------------

    fun enterWhileLoop(loop: FirLoop) {
        val (loopEnterNode, loopConditionEnterNode) = graphBuilder.enterWhileLoop(loop)
        loopEnterNode.mergeIncomingFlow()
        loopConditionEnterNode.mergeIncomingFlow()
    }

    fun exitWhileLoopCondition(loop: FirLoop) {
        val (loopConditionExitNode, loopBlockEnterNode) = graphBuilder.exitWhileLoopCondition(loop)
        loopConditionExitNode.mergeIncomingFlow()
        loopBlockEnterNode.mergeIncomingFlow()
    }

    fun exitWhileLoop(loop: FirLoop) {
        val (blockExitNode, exitNode) = graphBuilder.exitWhileLoop(loop)
        blockExitNode.mergeIncomingFlow()
        exitNode.mergeIncomingFlow()
    }

    // ----------------------------------- Do while Loop -----------------------------------

    fun enterDoWhileLoop(loop: FirLoop) {
        val (loopEnterNode, loopBlockEnterNode) = graphBuilder.enterDoWhileLoop(loop)
        loopEnterNode.mergeIncomingFlow()
        loopBlockEnterNode.mergeIncomingFlow()
    }

    fun enterDoWhileLoopCondition(loop: FirLoop) {
        val (loopBlockExitNode, loopConditionEnterNode) = graphBuilder.enterDoWhileLoopCondition(loop)
        loopBlockExitNode.mergeIncomingFlow()
        loopConditionEnterNode.mergeIncomingFlow()
    }

    fun exitDoWhileLoop(loop: FirLoop) {
        val (loopConditionExitNode, loopExitNode) = graphBuilder.exitDoWhileLoop(loop)
        loopConditionExitNode.mergeIncomingFlow()
        loopExitNode.mergeIncomingFlow()
    }

    // ----------------------------------- Try-catch-finally -----------------------------------

    fun enterTryExpression(tryExpression: FirTryExpression) {
        val (tryExpressionEnterNode, tryMainBlockEnterNode) = graphBuilder.enterTryExpression(tryExpression)
        tryExpressionEnterNode.mergeIncomingFlow()
        tryMainBlockEnterNode.mergeIncomingFlow()
    }

    fun exitTryMainBlock(tryExpression: FirTryExpression) {
        graphBuilder.exitTryMainBlock(tryExpression).mergeIncomingFlow()
    }

    fun enterCatchClause(catch: FirCatch) {
        graphBuilder.enterCatchClause(catch).mergeIncomingFlow()
    }

    fun exitCatchClause(catch: FirCatch) {
        graphBuilder.exitCatchClause(catch).mergeIncomingFlow()
    }

    fun enterFinallyBlock(tryExpression: FirTryExpression) {
        // TODO
        graphBuilder.enterFinallyBlock(tryExpression).mergeIncomingFlow()
    }

    fun exitFinallyBlock(tryExpression: FirTryExpression) {
        // TODO
        graphBuilder.exitFinallyBlock(tryExpression).mergeIncomingFlow()
    }

    fun exitTryExpression(tryExpression: FirTryExpression) {
        // TODO
        graphBuilder.exitTryExpression(tryExpression).mergeIncomingFlow()
    }

    // ----------------------------------- Resolvable call -----------------------------------

    fun exitQualifiedAccessExpression(qualifiedAccessExpression: FirQualifiedAccessExpression) {
        graphBuilder.exitQualifiedAccessExpression(qualifiedAccessExpression).mergeIncomingFlow()
    }

    fun enterFunctionCall(functionCall: FirFunctionCall) {
        // TODO: add processing in-place lambdas
    }

    fun exitFunctionCall(functionCall: FirFunctionCall) {
        val node = graphBuilder.exitFunctionCall(functionCall).mergeIncomingFlow()
        if (functionCall.isBooleanNot()) {
            exitBooleanNot(functionCall, node)
            return
        }
    }

    private val FirElement.resolvedSymbol: AbstractFirBasedSymbol<*>?
        get() {
            val expression = (this as? FirWhenSubjectExpression)?.whenSubject?.whenExpression?.let {
                it.subjectVariable?.symbol?.let { symbol -> return symbol }
                it.subject
            } ?: this
            return (expression as? FirResolvable)?.resolvedSymbol
        }

    private val FirResolvable.resolvedSymbol: AbstractFirBasedSymbol<*>?
        get() = (calleeReference as? FirResolvedCallableReference)?.resolvedSymbol

    private fun FirFunctionCall.isBooleanNot(): Boolean {
        val symbol = calleeReference.safeAs<FirResolvedCallableReference>()?.resolvedSymbol as? FirNamedFunctionSymbol ?: return false
        return symbol.callableId == KOTLIN_BOOLEAN_NOT
    }

    fun exitConstExpresion(constExpression: FirConstExpression<*>) {
        graphBuilder.exitConstExpresion(constExpression).mergeIncomingFlow()
    }

    fun exitVariableDeclaration(variable: FirVariable<*>) {
        val node = graphBuilder.exitVariableDeclaration(variable).mergeIncomingFlow()
        val initializer = variable.initializer ?: return

        /*
         * That part is needed for cases like that:
         *
         *   val b = x is String
         *   ...
         *   if (b) {
         *      x.length
         *   }
         */
        variableStorage[initializer]?.let { initializerVariable ->
            assert(initializerVariable.isSynthetic())
            val realVariable = getOrCreateRealVariable(variable.symbol)
            logicSystem.changeVariableForConditionFlow(node.flow, initializerVariable, realVariable)
        }

        initializer.resolvedSymbol?.let { initializerSymbol: AbstractFirBasedSymbol<*> ->
            val rhsVariable = getOrCreateRealVariable(initializerSymbol)
            variableStorage.createAliasVariable(variable.symbol, rhsVariable)
        }
    }

    fun exitVariableAssignment(assignment: FirVariableAssignment) {
        graphBuilder.exitVariableAssignment(assignment).mergeIncomingFlow()
        val lhsVariable = variableStorage[assignment.resolvedSymbol ?: return] ?: return
        val rhsVariable = variableStorage[assignment.rValue.resolvedSymbol ?: return]?.takeIf { !it.isSynthetic() } ?: return
        variableStorage.rebindAliasVariable(lhsVariable, rhsVariable)
    }

    fun exitThrowExceptionNode(throwExpression: FirThrowExpression) {
        graphBuilder.exitThrowExceptionNode(throwExpression).mergeIncomingFlow()
    }

    // ----------------------------------- Boolean operators -----------------------------------

    fun enterBinaryAnd(binaryLogicExpression: FirBinaryLogicExpression) {
        graphBuilder.enterBinaryAnd(binaryLogicExpression).mergeIncomingFlow()
    }

    fun exitLeftBinaryAndArgument(binaryLogicExpression: FirBinaryLogicExpression) {
        val (leftNode, rightNode) = graphBuilder.exitLeftBinaryAndArgument(binaryLogicExpression)
        exitLeftArgumentOfBinaryBooleanOperator(leftNode, rightNode, isAnd = true)
    }

    fun exitBinaryAnd(binaryLogicExpression: FirBinaryLogicExpression) {
        val node = graphBuilder.exitBinaryAnd(binaryLogicExpression)
        exitBinaryBooleanOperator(binaryLogicExpression, node, isAnd = true)
    }

    fun enterBinaryOr(binaryLogicExpression: FirBinaryLogicExpression) {
        graphBuilder.enterBinaryOr(binaryLogicExpression).mergeIncomingFlow()
    }

    fun exitLeftBinaryOrArgument(binaryLogicExpression: FirBinaryLogicExpression) {
        val (leftNode, rightNode) = graphBuilder.exitLeftBinaryOrArgument(binaryLogicExpression)
        exitLeftArgumentOfBinaryBooleanOperator(leftNode, rightNode, isAnd = false)
    }

    fun exitBinaryOr(binaryLogicExpression: FirBinaryLogicExpression) {
        val node = graphBuilder.exitBinaryOr(binaryLogicExpression)
        exitBinaryBooleanOperator(binaryLogicExpression, node, isAnd = false)
    }

    private fun exitLeftArgumentOfBinaryBooleanOperator(leftNode: CFGNode<*>, rightNode: CFGNode<*>, isAnd: Boolean) {
        val parentFlow = leftNode.alivePreviousNodes.first().flow
        leftNode.flow = logicSystem.forkFlow(parentFlow)
        val leftOperandVariable = getOrCreateVariable(leftNode.previousNodes.first().fir)
        rightNode.flow = logicSystem.approveFactsInsideFlow(
            leftOperandVariable,
            if (isAnd) EqTrue else EqFalse,
            parentFlow,
            shouldForkFlow = true,
            shouldRemoveSynthetics = false
        )
    }

    private fun exitBinaryBooleanOperator(
        binaryLogicExpression: FirBinaryLogicExpression,
        node: AbstractBinaryExitNode<*>,
        isAnd: Boolean
    ) {
        val bothEvaluated = if (isAnd) EqTrue else EqFalse
        val onlyLeftEvaluated = bothEvaluated.invert()

        // Naming for all variables was chosen in assumption that we processing && expression
        val flowFromLeft = node.leftOperandNode.flow
        val flowFromRight = node.rightOperandNode.flow

        val flow = node.mergeIncomingFlow().flow

        val (leftVariable, rightVariable) = binaryLogicExpression.getVariables()
        val andVariable = getOrCreateVariable(binaryLogicExpression)

        val (conditionalFromLeft, conditionalFromRight, approvedFromRight) = logicSystem.collectInfoForBooleanOperator(
            flowFromLeft,
            leftVariable,
            flowFromRight,
            rightVariable
        )

        // left && right == True
        // left || right == False
        val approvedIfTrue: MutableApprovedInfos = mutableMapOf()
        logicSystem.approveFactTo(approvedIfTrue, bothEvaluated, conditionalFromLeft)
        logicSystem.approveFactTo(approvedIfTrue, bothEvaluated, conditionalFromRight)
        approvedFromRight.forEach { (variable, info) ->
            approvedIfTrue.addInfo(variable, info)
        }
        approvedIfTrue.forEach { (variable, info) ->
            logicSystem.addConditionalInfo(flow, andVariable, info.toConditional(bothEvaluated, variable))
        }

        // left && right == False
        // left || right == True
        val approvedIfFalse: MutableApprovedInfos = mutableMapOf()
        val leftIsFalse = logicSystem.approveFact(onlyLeftEvaluated, conditionalFromLeft)
        val rightIsFalse = logicSystem.approveFact(onlyLeftEvaluated, conditionalFromRight)
        approvedIfFalse.mergeInfo(logicSystem.orForVerifiedFacts(leftIsFalse, rightIsFalse))
        approvedIfFalse.forEach { (variable, info) ->
            logicSystem.addConditionalInfo(flow, andVariable, info.toConditional(onlyLeftEvaluated, variable))
        }

        node.flow = flow

        variableStorage.removeVariableIfSynthetic(leftVariable)
        variableStorage.removeVariableIfSynthetic(rightVariable)
    }


    private fun exitBooleanNot(functionCall: FirFunctionCall, node: FunctionCallNode) {
        val booleanExpressionVariable = getOrCreateVariable(node.previousNodes.first().fir)
        val variable = getOrCreateVariable(functionCall)
        logicSystem.changeVariableForConditionFlow(node.flow, booleanExpressionVariable, variable) { it.invert() }
    }

    // ----------------------------------- Annotations -----------------------------------

    fun enterAnnotationCall(annotationCall: FirAnnotationCall) {
        graphBuilder.enterAnnotationCall(annotationCall).mergeIncomingFlow()
    }

    fun exitAnnotationCall(annotationCall: FirAnnotationCall) {
        graphBuilder.exitAnnotationCall(annotationCall).mergeIncomingFlow()
    }

    // ----------------------------------- Init block -----------------------------------

    fun enterInitBlock(initBlock: FirAnonymousInitializer) {
        graphBuilder.enterInitBlock(initBlock).mergeIncomingFlow()
    }

    fun exitInitBlock(initBlock: FirAnonymousInitializer) {
        graphBuilder.exitInitBlock(initBlock).mergeIncomingFlow()
    }

    // -------------------------------------------------------------------------------------------------------------------------

    private fun FirBinaryLogicExpression.getVariables(): Pair<DataFlowVariable, DataFlowVariable> =
        getOrCreateVariable(leftOperand) to getOrCreateVariable(rightOperand)

    private var CFGNode<*>.flow: Flow
        get() = flowOnNodes.getValue(this.origin)
        set(value) {
            flowOnNodes[this.origin] = value
        }

    private val CFGNode<*>.origin: CFGNode<*> get() = if (this is StubNode) previousNodes.first() else this

    private fun <T : CFGNode<*>> T.mergeIncomingFlow(): T = this.also { node ->
        val previousFlows = node.alivePreviousNodes.map { it.flow }
        node.flow = logicSystem.joinFlow(previousFlows)
    }

    // -------------------------------- get or create variable --------------------------------

    private fun getOrCreateSyntheticVariable(fir: FirElement): SyntheticDataFlowVariable =
        variableStorage.getOrCreateNewSyntheticVariable(fir)

    private fun getOrCreateRealVariable(fir: FirElement): RealDataFlowVariable? {
        if (fir is FirThisReceiverExpressionImpl) {
            return variableStorage.getOrCreateNewThisRealVariable(fir.calleeReference.boundSymbol ?: return null)
        }
        val symbol = fir.resolvedSymbol ?: return null
        return variableStorage.getOrCreateNewRealVariable(symbol)
    }

    private fun getOrCreateRealVariable(symbol: AbstractFirBasedSymbol<*>): RealDataFlowVariable =
        variableStorage.getOrCreateNewRealVariable(symbol).variableUnderAlias

    private fun getOrCreateVariable(fir: FirElement): DataFlowVariable {
        val symbol = fir.resolvedSymbol
        return if (symbol == null)
            getOrCreateSyntheticVariable(fir)
        else
            getOrCreateRealVariable(symbol)
    }

    // -------------------------------- get variable --------------------------------

    private val FirElement.realVariable: RealDataFlowVariable?
        get() {
            val symbol = if (this is FirThisReceiverExpressionImpl) {
                calleeReference.boundSymbol
            } else {
                resolvedSymbol
            } ?: return null
            return variableStorage[symbol]
        }

    private fun getRealVariablesForSafeCallChain(call: FirExpression): Collection<RealDataFlowVariable> {
        val result = mutableListOf<RealDataFlowVariable>()

        fun collect(call: FirExpression) {
            when (call) {
                is FirQualifiedAccess -> {
                    if (call.safe) {
                        val explicitReceiver = call.explicitReceiver
                        require(explicitReceiver != null)
                        collect(explicitReceiver)
                    }
                    ((call.calleeReference as? FirResolvedCallableReference)?.resolvedSymbol)?.let { symbol ->
                        if (symbol is FirVariableSymbol<*> || symbol is FirPropertySymbol) {
                            result += getOrCreateRealVariable(symbol)
                        }
                    }
                }
                is FirWhenSubjectExpression -> {
                    // TODO: check
                    call.whenSubject.whenExpression.subjectVariable?.let { result += getOrCreateRealVariable(it.symbol) }
                    call.whenSubject.whenExpression.subject?.let { collect(it) }
                }
            }
        }

        collect(call)
        return result
    }

    private inner class LogicSystemImpl(context: DataFlowInferenceContext) : DelegatingLogicSystem(context) {
        override fun processUpdatedReceiverVariable(flow: Flow, variable: RealDataFlowVariable) {
            val symbol = (variable.fir as? FirSymbolOwner<*>)?.symbol ?: return

            val index = receiverStack.getReceiverIndex(symbol) ?: return
            val info = flow.getApprovedInfo(variable)

            if (info == null) {
                receiverStack.replaceReceiverType(index, receiverStack.getOriginalType(index))
            } else {
                val types = info.exactType.toMutableList().also {
                    it += receiverStack.getOriginalType(index)
                }
                receiverStack.replaceReceiverType(index, context.intersectTypesOrNull(types)!!)
            }
        }

        override fun updateAllReceivers(flow: Flow) {
            receiverStack.mapNotNull { variableStorage[it.boundSymbol] }.forEach { processUpdatedReceiverVariable(flow, it) }
        }
    }
}