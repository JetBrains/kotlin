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
import org.jetbrains.kotlin.fir.resolve.BodyResolveComponents
import org.jetbrains.kotlin.fir.resolve.dfa.Condition.*
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.*
import org.jetbrains.kotlin.fir.resolve.transformers.FirBodyResolveTransformer
import org.jetbrains.kotlin.fir.symbols.CallableId
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirVariableSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.ir.expressions.IrConstKind
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

class FirDataFlowAnalyzer(transformer: FirBodyResolveTransformer) : BodyResolveComponents by transformer {
    companion object {
        private val KOTLIN_BOOLEAN_NOT = CallableId(FqName("kotlin"), FqName("Boolean"), Name.identifier("not"))
    }

    private val context: DataFlowInferenceContext get() = inferenceComponents.ctx as DataFlowInferenceContext

    private val graphBuilder = ControlFlowGraphBuilder()
    private val logicSystem = LogicSystem(context)
    private val variableStorage = DataFlowVariableStorage()
    private val edges = mutableMapOf<CFGNode<*>, Flow>().withDefault { Flow.EMPTY }

    /*
     * If there is no types from smartcasts function returns null
     */
    fun getTypeUsingSmartcastInfo(qualifiedAccessExpression: FirQualifiedAccessExpression): Collection<ConeKotlinType>? {
        val symbol: FirBasedSymbol<*> = qualifiedAccessExpression.resolvedSymbol ?: return null
        val variable = variableStorage[symbol]?.real ?: return null
        return graphBuilder.lastNode.flow.approvedFacts(variable)?.exactType ?: return null
    }

    // ----------------------------------- Named function -----------------------------------

    fun enterFunction(function: FirFunction<*>) {
        graphBuilder.enterFunction(function).passFlow()

        for (valueParameter in function.valueParameters) {
            getRealVariable(valueParameter.symbol)
        }
    }

    fun exitFunction(function: FirFunction<*>): ControlFlowGraph? {
        val (node, graph) = graphBuilder.exitFunction(function)
        node.passFlow()
        for (valueParameter in function.valueParameters) {
            variableStorage.removeRealVariable(valueParameter.symbol)
        }
        if (graphBuilder.isTopLevel()) {
            edges.clear()
            variableStorage.reset()
        }
        return graph
    }

    // ----------------------------------- Property -----------------------------------

    fun enterProperty(property: FirProperty) {
        graphBuilder.enterProperty(property).passFlow()
    }

    fun exitProperty(property: FirProperty): ControlFlowGraph {
        val (node, graph) = graphBuilder.exitProperty(property)
        node.passFlow()
        return graph
    }

    // ----------------------------------- Block -----------------------------------

    fun enterBlock(block: FirBlock) {
        val node = graphBuilder.enterBlock(block).passFlow(false)

        val previousNode = node.usefulPreviousNodes.singleOrNull() as? WhenBranchConditionExitNode
        if (previousNode != null) {
            node.flow = logicSystem.approveFactsInsideFlow(previousNode.variable, EqTrue, node.flow)
        }
        node.flow.freeze()
    }

    fun exitBlock(block: FirBlock) {
        graphBuilder.exitBlock(block).passFlow()
    }

    // ----------------------------------- Operator call -----------------------------------

    private fun FirExpression.getResolvedSymbol(): FirCallableSymbol<*>? {
        val expression = (this as? FirWhenSubjectExpression)?.whenSubject?.whenExpression?.let {
            it.subjectVariable?.symbol?.let { return it }
            it.subject
        } ?: this
        return expression.toResolvedCallableSymbol() as? FirCallableSymbol<*>
    }

    fun exitTypeOperatorCall(typeOperatorCall: FirTypeOperatorCall) {
        val node = graphBuilder.exitTypeOperatorCall(typeOperatorCall).passFlow(false)
        try {
            if (typeOperatorCall.operation !in FirOperation.TYPES) return
            val symbol: FirCallableSymbol<*> = typeOperatorCall.argument.getResolvedSymbol() ?: return
            val type = typeOperatorCall.conversionTypeRef.coneTypeSafe<ConeKotlinType>() ?: return
            val varVariable = getRealVariable(symbol)

            var flow = node.flow
            when (typeOperatorCall.operation) {
                FirOperation.IS, FirOperation.NOT_IS -> {
                    val expressionVariable = getSyntheticVariable(typeOperatorCall)

                    val trueInfo = FirDataFlowInfo(setOf(type), emptySet())
                    val falseInfo = FirDataFlowInfo(emptySet(), setOf(type))

                    fun chooseInfo(trueBranch: Boolean) =
                        if ((typeOperatorCall.operation == FirOperation.IS) == trueBranch) trueInfo else falseInfo

                    flow = flow.addNotApprovedFact(
                        expressionVariable,
                        UnapprovedFirDataFlowInfo(
                            EqTrue, varVariable, chooseInfo(true)
                        )
                    )

                    flow = flow.addNotApprovedFact(
                        expressionVariable,
                        UnapprovedFirDataFlowInfo(
                            EqFalse, varVariable, chooseInfo(false)
                        )
                    )
                }

                FirOperation.AS -> {
                    flow = flow.addApprovedFact(varVariable, FirDataFlowInfo(setOf(type), emptySet()))
                }

                FirOperation.SAFE_AS -> {
                    val expressionVariable = getSyntheticVariable(typeOperatorCall)
                    flow = flow.addNotApprovedFact(
                        expressionVariable,
                        UnapprovedFirDataFlowInfo(
                            NotEqNull, varVariable, FirDataFlowInfo(setOf(type), emptySet())
                        )
                    ).addNotApprovedFact(
                        expressionVariable,
                        UnapprovedFirDataFlowInfo(
                            EqNull, varVariable, FirDataFlowInfo(emptySet(), setOf(type))
                        )
                    )
                }

                else -> throw IllegalStateException()
            }

            node.flow = flow
        } finally {
            node.flow.freeze()
        }
    }

    fun exitOperatorCall(operatorCall: FirOperatorCall) {
        val node = graphBuilder.exitOperatorCall(operatorCall).passFlow(false)
        try {
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
        } finally {
            node.flow.freeze()
        }
    }

    private fun processEqWithConst(node: OperatorCallNode, operand: FirExpression, const: FirConstExpression<*>, operation: FirOperation) {
        val isEq = when (operation) {
            FirOperation.EQ, FirOperation.IDENTITY -> true
            FirOperation.NOT_EQ, FirOperation.NOT_IDENTITY -> false
            else -> return
        }

        val expressionVariable = getVariable(node.fir)
        var flow = node.flow

        // not null for comparisons with constants
        getRealVariablesForSafeCallChain(operand).takeIf { it.isNotEmpty() }?.let { operandVariables ->
            operandVariables.forEach { operandVariable ->
                flow = flow.addNotApprovedFact(
                    expressionVariable, UnapprovedFirDataFlowInfo(
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

            flow.notApprovedFacts[operandVariable].forEach { info ->
                flow = flow.addNotApprovedFact(expressionVariable, info.let { if (shouldInvert) it.invert() else it })
            }
        }

        node.flow = flow
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
        var flow = node.flow
        val expressionVariable = getVariable(node.fir)

        variableStorage[operand]?.let { operandVariable ->
            val condition = when (operation) {
                FirOperation.EQ, FirOperation.IDENTITY -> EqNull
                FirOperation.NOT_EQ, FirOperation.NOT_IDENTITY -> NotEqNull
                else -> throw IllegalArgumentException()
            }
            val facts = logicSystem.approveFact(operandVariable, condition, flow)
            facts.forEach { (variable, info) ->
                flow = flow.addNotApprovedFact(
                    expressionVariable,
                    UnapprovedFirDataFlowInfo(
                        EqTrue, variable, info
                    )
                ).addNotApprovedFact(
                    expressionVariable,
                    UnapprovedFirDataFlowInfo(
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
            flow = flow.addNotApprovedFact(
                expressionVariable, UnapprovedFirDataFlowInfo(
                    condition,
                    operandVariable,
                    FirDataFlowInfo(setOf(session.builtinTypes.anyType.coneTypeUnsafe()), emptySet())
                )
            )
            // TODO: design do we need casts to Nothing?
            /*
            flow = addNotApprovedFact(
                expressionVariable, UnapprovedFirDataFlowInfo(
                    eq(conditionValue.invert()!!),
                    operandVariable,
                    FirDataFlowInfo(setOf(session.builtinTypes.nullableNothingType.coneTypeUnsafe()), emptySet())
                )
            )
            */

        }
        node.flow = flow
    }

    // ----------------------------------- Jump -----------------------------------

    fun exitJump(jump: FirJump<*>) {
        graphBuilder.exitJump(jump).passFlow()
    }

    // ----------------------------------- When -----------------------------------

    fun enterWhenExpression(whenExpression: FirWhenExpression) {
        graphBuilder.enterWhenExpression(whenExpression).passFlow()
    }

    fun enterWhenBranchCondition(whenBranch: FirWhenBranch) {
        val node = graphBuilder.enterWhenBranchCondition(whenBranch).passFlow(false)
        val previousNode = node.previousNodes.single()
        if (previousNode is WhenBranchConditionExitNode) {
            node.flow = logicSystem.approveFactsInsideFlow(previousNode.variable, EqFalse, node.flow)
        }
        node.flow.freeze()
    }

    fun exitWhenBranchCondition(whenBranch: FirWhenBranch) {
        val node = graphBuilder.exitWhenBranchCondition(whenBranch).passFlow()

        val conditionVariable = getVariable(whenBranch.condition)
        node.variable = conditionVariable
    }

    fun exitWhenBranchResult(whenBranch: FirWhenBranch) {
        val node = graphBuilder.exitWhenBranchResult(whenBranch).passFlow(false)
        val conditionVariable = getVariable(whenBranch.condition)
        node.flow = node.flow.removeSyntheticVariable(conditionVariable)
    }

    fun exitWhenExpression(whenExpression: FirWhenExpression) {
        val node = graphBuilder.exitWhenExpression(whenExpression)
        var flow = logicSystem.or(node.usefulPreviousNodes.map { it.flow })
        val subjectSymbol = whenExpression.subjectVariable?.symbol
        if (subjectSymbol != null) {
            variableStorage[subjectSymbol]?.let { flow = flow.removeVariable(it) }
        }
        node.flow = flow
    }

    // ----------------------------------- While Loop -----------------------------------

    fun enterWhileLoop(loop: FirLoop) {
        val (loopEnterNode, loopConditionEnterNode) = graphBuilder.enterWhileLoop(loop)
        loopEnterNode.passFlow()
        loopConditionEnterNode.passFlow()
    }

    fun exitWhileLoopCondition(loop: FirLoop) {
        graphBuilder.exitWhileLoopCondition(loop).passFlow()
    }

    fun exitWhileLoop(loop: FirLoop) {
        graphBuilder.exitWhileLoop(loop).also { (blockExitNode, exitNode) ->
            blockExitNode.passFlow()
            exitNode.passFlow()
        }
    }

    // ----------------------------------- Do while Loop -----------------------------------

    fun enterDoWhileLoop(loop: FirLoop) {
        graphBuilder.enterDoWhileLoop(loop).passFlow()
    }

    fun enterDoWhileLoopCondition(loop: FirLoop) {
        val (loopBlockExitNode, loopConditionEnterNode) = graphBuilder.enterDoWhileLoopCondition(loop)
        loopBlockExitNode.passFlow()
        loopConditionEnterNode.passFlow()
    }

    fun exitDoWhileLoop(loop: FirLoop) {
        graphBuilder.exitDoWhileLoop(loop).passFlow()
    }

    // ----------------------------------- Try-catch-finally -----------------------------------

    fun enterTryExpression(tryExpression: FirTryExpression) {
        graphBuilder.enterTryExpression(tryExpression).passFlow()
    }

    fun exitTryMainBlock(tryExpression: FirTryExpression) {
        graphBuilder.exitTryMainBlock(tryExpression).passFlow()
    }

    fun enterCatchClause(catch: FirCatch) {
        graphBuilder.enterCatchClause(catch).passFlow()
    }

    fun exitCatchClause(catch: FirCatch) {
        graphBuilder.exitCatchClause(catch).passFlow()
    }

    fun enterFinallyBlock(tryExpression: FirTryExpression) {
        // TODO
        graphBuilder.enterFinallyBlock(tryExpression).passFlow()
    }

    fun exitFinallyBlock(tryExpression: FirTryExpression) {
        // TODO
        graphBuilder.exitFinallyBlock(tryExpression).passFlow()
    }

    fun exitTryExpression(tryExpression: FirTryExpression) {
        // TODO
        graphBuilder.exitTryExpression(tryExpression).passFlow()
    }

    // ----------------------------------- Resolvable call -----------------------------------

    fun exitQualifiedAccessExpression(qualifiedAccessExpression: FirQualifiedAccessExpression) {
        graphBuilder.exitQualifiedAccessExpression(qualifiedAccessExpression).passFlow()
    }

    fun enterFunctionCall(functionCall: FirFunctionCall) {
        // TODO: add processing in-place lambdas
    }

    fun exitFunctionCall(functionCall: FirFunctionCall) {
        val node = graphBuilder.exitFunctionCall(functionCall).passFlow(false)
        if (functionCall.isBooleanNot()) {
            exitBooleanNot(functionCall, node)
            return
        }
    }

    private val FirElement.resolvedSymbol: FirBasedSymbol<*>? get() = this.safeAs<FirResolvable>()?.calleeReference.safeAs<FirResolvedCallableReference>()?.coneSymbol as? FirBasedSymbol<*>

    private fun FirFunctionCall.isBooleanNot(): Boolean {
        val symbol = calleeReference.safeAs<FirResolvedCallableReference>()?.coneSymbol as? FirNamedFunctionSymbol ?: return false
        return symbol.callableId == KOTLIN_BOOLEAN_NOT
    }

    fun exitConstExpresion(constExpression: FirConstExpression<*>) {
        graphBuilder.exitConstExpresion(constExpression).passFlow()
    }

    fun exitVariableDeclaration(variable: FirVariable<*>) {
        val node = graphBuilder.exitVariableDeclaration(variable).passFlow(false)
        try {
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
                assert(initializerVariable.isSynthetic)
                val realVariable = getRealVariable(variable.symbol)
                node.flow = node.flow.copyNotApprovedFacts(initializerVariable, realVariable)
            }

            initializer.resolvedSymbol?.let { initializerSymbol: FirBasedSymbol<*> ->
                val rhsVariable = variableStorage[initializerSymbol]?.takeIf { !it.isSynthetic } ?: return
                variableStorage.createAliasVariable(variable.symbol, rhsVariable)
            }

        } finally {
            node.flow.freeze()
        }
    }

    fun exitVariableAssignment(assignment: FirVariableAssignment) {
        graphBuilder.exitVariableAssignment(assignment).passFlow()
        val lhsVariable = variableStorage[assignment.resolvedSymbol ?: return] ?: return
        val rhsVariable = variableStorage[assignment.rValue.resolvedSymbol ?: return]?.takeIf { !it.isSynthetic } ?: return
        variableStorage.rebindAliasVariable(lhsVariable, rhsVariable)
    }

    fun exitThrowExceptionNode(throwExpression: FirThrowExpression) {
        graphBuilder.exitThrowExceptionNode(throwExpression).passFlow()
    }

    // ----------------------------------- Boolean operators -----------------------------------

    fun enterBinaryAnd(binaryLogicExpression: FirBinaryLogicExpression) {
        graphBuilder.enterBinaryAnd(binaryLogicExpression).passFlow()
    }

    fun exitLeftBinaryAndArgument(binaryLogicExpression: FirBinaryLogicExpression) {
        val (leftNode, rightNode) = graphBuilder.exitLeftBinaryAndArgument(binaryLogicExpression)
        leftNode.passFlow()
        rightNode.passFlow(false)
        val leftOperandVariable = getVariable(leftNode.previousNodes.first().fir)
        rightNode.flow = logicSystem.approveFactsInsideFlow(leftOperandVariable, EqTrue, rightNode.flow).also { it.freeze() }
    }

    fun exitBinaryAnd(binaryLogicExpression: FirBinaryLogicExpression) {
        val node = graphBuilder.exitBinaryAnd(binaryLogicExpression).passFlow(false)
        val (leftVariable, rightVariable) = binaryLogicExpression.getVariables()

        val flowFromLeft = node.leftOperandNode.flow
        val flowFromRight = node.rightOperandNode.flow
        val flow = node.flow

        val andVariable = getVariable(binaryLogicExpression)

        val leftIsTrue = approveFact(leftVariable, EqTrue, flowFromRight)
        val leftIsFalse = approveFact(leftVariable, EqFalse, flowFromRight)
        val rightIsTrue = approveFact(rightVariable, EqTrue, flowFromRight) ?: mutableMapOf()
        val rightIsFalse = approveFact(rightVariable, EqFalse, flowFromRight)

        flowFromRight.approvedFacts.forEach { (variable, info) ->
            val actualInfo = flowFromLeft.approvedFacts[variable]?.let { info - it } ?: info
            if (actualInfo.isNotEmpty) rightIsTrue.compute(variable) { _, existingInfo -> info + existingInfo }
        }

        logicSystem.andForVerifiedFacts(leftIsTrue, rightIsTrue)?.let {
            for ((variable, info) in it) {
                flow.addNotApprovedFact(andVariable, UnapprovedFirDataFlowInfo(EqTrue, variable, info))
            }
        }
        logicSystem.orForVerifiedFacts(leftIsFalse, rightIsFalse)?.let {
            for ((variable, info) in it) {
                flow.addNotApprovedFact(andVariable, UnapprovedFirDataFlowInfo(EqFalse, variable, info))
            }
        }
        node.flow = flow.removeSyntheticVariable(leftVariable).removeSyntheticVariable(rightVariable).also { it.freeze() }
    }

    fun enterBinaryOr(binaryLogicExpression: FirBinaryLogicExpression) {
        graphBuilder.enterBinaryOr(binaryLogicExpression)
    }

    fun exitLeftBinaryOrArgument(binaryLogicExpression: FirBinaryLogicExpression) {
        val (leftNode, rightNode) = graphBuilder.exitLeftBinaryOrArgument(binaryLogicExpression)
        leftNode.passFlow()
        rightNode.passFlow(false)
        val leftOperandVariable = getVariable(leftNode.previousNodes.first().fir)
        rightNode.flow = logicSystem.approveFactsInsideFlow(leftOperandVariable, EqFalse, rightNode.flow).also { it.freeze() }
    }

    fun exitBinaryOr(binaryLogicExpression: FirBinaryLogicExpression) {
        val node = graphBuilder.exitBinaryOr(binaryLogicExpression).passFlow(false)
        val (leftVariable, rightVariable) = binaryLogicExpression.getVariables()

        val flowFromLeft = node.leftOperandNode.flow
        val flowFromRight = node.rightOperandNode.flow
        val flow = node.flow

        val orVariable = getVariable(binaryLogicExpression)

        val leftIsTrue = approveFact(leftVariable, EqTrue, flowFromLeft)
        val leftIsFalse = approveFact(leftVariable, EqFalse, flowFromLeft)
        val rightIsTrue = approveFact(rightVariable, EqTrue, flowFromRight)
        val rightIsFalse = approveFact(rightVariable, EqFalse, flowFromRight)

        logicSystem.orForVerifiedFacts(leftIsTrue, rightIsTrue)?.let {
            for ((variable, info) in it) {
                flow.addNotApprovedFact(orVariable, UnapprovedFirDataFlowInfo(EqTrue, variable, info))
            }
        }
        logicSystem.andForVerifiedFacts(leftIsFalse, rightIsFalse)?.let {
            for ((variable, info) in it) {
                flow.addNotApprovedFact(orVariable, UnapprovedFirDataFlowInfo(EqFalse, variable, info))
            }
        }
        node.flow = flow.removeSyntheticVariable(leftVariable).removeSyntheticVariable(rightVariable).also { it.freeze() }
    }

    private fun exitBooleanNot(functionCall: FirFunctionCall, node: FunctionCallNode) {
        val booleanExpressionVariable = getVariable(node.previousNodes.first().fir)
        val variable = getVariable(functionCall)
        node.flow = node.flow.copyNotApprovedFacts(booleanExpressionVariable, variable) { it.invert() }.also { it.freeze() }
    }

    // ----------------------------------- Annotations -----------------------------------

    fun enterAnnotationCall(annotationCall: FirAnnotationCall) {
        graphBuilder.enterAnnotationCall(annotationCall).passFlow()
    }

    fun exitAnnotationCall(annotationCall: FirAnnotationCall) {
        graphBuilder.exitAnnotationCall(annotationCall).passFlow()
    }

    // ----------------------------------- Init block -----------------------------------

    fun enterInitBlock(initBlock: FirAnonymousInitializer) {
        graphBuilder.enterInitBlock(initBlock)
    }

    fun exitInitBlock(initBlock: FirAnonymousInitializer) {
        graphBuilder.exitInitBlock(initBlock).passFlow()
    }

    // -------------------------------------------------------------------------------------------------------------------------

    private fun approveFact(variable: DataFlowVariable, condition: Condition, flow: Flow): MutableMap<DataFlowVariable, FirDataFlowInfo>? =
        logicSystem.approveFact(variable, condition, flow)

    private fun FirBinaryLogicExpression.getVariables(): Pair<DataFlowVariable, DataFlowVariable> =
        getVariable(leftOperand) to getVariable(rightOperand)

    private var CFGNode<*>.flow: Flow
        get() = edges.getValue(this)
        set(value) {
            edges[this] = value
        }

    private fun <T : CFGNode<*>> T.passFlow(shouldFreeze: Boolean = true): T = this.also { node ->
        node.flow = logicSystem.or(node.usefulPreviousNodes.map { it.flow }).also {
            if (shouldFreeze) it.freeze()
        }
    }

    private fun getSyntheticVariable(fir: FirElement): DataFlowVariable = variableStorage.getOrCreateNewSyntheticVariable(fir)
    private fun getRealVariable(symbol: FirBasedSymbol<*>): DataFlowVariable = variableStorage.getOrCreateNewRealVariable(symbol).real

    private fun getVariable(fir: FirElement): DataFlowVariable {
        val symbol = fir.resolvedSymbol
        return if (symbol == null)
            getSyntheticVariable(fir)
        else
            getRealVariable(symbol)
    }

    private fun getRealVariablesForSafeCallChain(call: FirExpression): Collection<DataFlowVariable> {
        val result = mutableListOf<DataFlowVariable>()

        fun collect(call: FirExpression) {
            when (call) {
                is FirQualifiedAccess -> {
                    if (call.safe) {
                        val explicitReceiver = call.explicitReceiver
                        require(explicitReceiver != null)
                        collect(explicitReceiver)
                    }
                    ((call.calleeReference as? FirResolvedCallableReference)?.coneSymbol)?.let { symbol ->
                        if (symbol is FirVariableSymbol<*> || symbol is FirPropertySymbol) {
                            result += getRealVariable(symbol as FirBasedSymbol<*>)
                        }
                    }
                }
                is FirWhenSubjectExpression -> {
                    call.whenSubject.whenExpression.subjectVariable?.let { result += getRealVariable(it.symbol) }
                    call.whenSubject.whenExpression.subject?.let { collect(it) }
                }
            }
        }

        collect(call)
        return result
    }

    private fun Flow.removeVariable(variable: DataFlowVariable): Flow {
        variableStorage.removeVariable(variable)
        return removeVariableFromFlow(variable)
    }

    private fun Flow.removeSyntheticVariable(variable: DataFlowVariable): Flow {
        if (!variable.isSynthetic) return this
        return removeVariable(variable)
    }
}