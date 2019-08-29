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
import org.jetbrains.kotlin.fir.resolve.dfa.ConditionValue.*
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

class FirDataFlowAnalyzerImpl(transformer: FirBodyResolveTransformer) : FirDataFlowAnalyzer(), BodyResolveComponents by transformer {
    companion object {
        private val KOTLIN_BOOLEAN_NOT = CallableId(FqName("kotlin"), FqName("Boolean"), Name.identifier("not"))
    }

    private val context: DataFlowInferenceContext get() = inferenceComponents.ctx as DataFlowInferenceContext

    private val graphBuilder = ControlFlowGraphBuilder()
    private val logicSystem = LogicSystem(context)
    private val variableStorage = DataFlowVariableStorage()
    private val edges = mutableMapOf<CFGNode<*>, Flow>().withDefault { Flow.EMPTY }

    override fun getTypeUsingSmartcastInfo(qualifiedAccessExpression: FirQualifiedAccessExpression): Collection<ConeKotlinType>? {
        val symbol: FirBasedSymbol<*> = qualifiedAccessExpression.resolvedSymbol ?: return null
        val variable = variableStorage[symbol]?.real ?: return null
        return graphBuilder.lastNode.flow.approvedFacts(variable)?.exactType ?: return null
    }

    // ----------------------------------- Named function -----------------------------------

    override fun enterFunction(function: FirFunction<*>) {
        graphBuilder.enterFunction(function).passFlow()

        for (valueParameter in function.valueParameters) {
            getRealVariable(valueParameter.symbol)
        }
    }

    override fun exitFunction(function: FirFunction<*>): ControlFlowGraph? {
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

    override fun enterProperty(property: FirProperty) {
        graphBuilder.enterProperty(property).passFlow()
    }

    override fun exitProperty(property: FirProperty): ControlFlowGraph {
        val (node, graph) = graphBuilder.exitProperty(property)
        node.passFlow()
        return graph
    }

    // ----------------------------------- Block -----------------------------------

    override fun enterBlock(block: FirBlock) {
        val node = graphBuilder.enterBlock(block).passFlow(false)

        val previousNode = node.usefulPreviousNodes.singleOrNull() as? WhenBranchConditionExitNode
        if (previousNode != null) {
            node.flow = logicSystem.approveFactsInsideFlow(previousNode.trueCondition, node.flow)
        }
        node.flow.freeze()
    }

    override fun exitBlock(block: FirBlock) {
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

    override fun exitTypeOperatorCall(typeOperatorCall: FirTypeOperatorCall) {
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

                    fun chooseInfo(trueBranch: Boolean) = if ((typeOperatorCall.operation == FirOperation.IS) == trueBranch) trueInfo else falseInfo

                    flow = flow.addNotApprovedFact(
                        expressionVariable,
                        UnapprovedFirDataFlowInfo(
                            eq(True), varVariable, chooseInfo(true)
                        )
                    )

                    flow = flow.addNotApprovedFact(
                        expressionVariable,
                        UnapprovedFirDataFlowInfo(
                            eq(False), varVariable, chooseInfo(false)
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
                            notEq(Null), varVariable, FirDataFlowInfo(setOf(type), emptySet())
                        )
                    ).addNotApprovedFact(
                        expressionVariable,
                        UnapprovedFirDataFlowInfo(
                            eq(Null), varVariable, FirDataFlowInfo(emptySet(), setOf(type))
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

    override fun exitOperatorCall(operatorCall: FirOperatorCall) {
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
                        eq(isEq.toConditionValue()),
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
            val operator = when (operation) {
                FirOperation.EQ, FirOperation.IDENTITY -> ConditionOperator.Eq
                FirOperation.NOT_EQ, FirOperation.NOT_IDENTITY -> ConditionOperator.NotEq
                else -> throw IllegalArgumentException()
            }
            val facts = logicSystem.approveFact(Condition(operandVariable, operator, Null), flow)
            facts.forEach { (variable, info) ->
                flow = flow.addNotApprovedFact(
                    expressionVariable,
                    UnapprovedFirDataFlowInfo(
                        eq(True), variable, info
                    )
                ).addNotApprovedFact(
                    expressionVariable,
                    UnapprovedFirDataFlowInfo(
                        eq(False), variable, info.invert()
                    )
                )
            }
            node.flow = flow
            return
        }

        val operandVariables = getRealVariablesForSafeCallChain(operand).takeIf { it.isNotEmpty() } ?: return

        val conditionValue = when (operation) {
            FirOperation.EQ, FirOperation.IDENTITY -> False
            FirOperation.NOT_EQ, FirOperation.NOT_IDENTITY -> True
            else -> throw IllegalArgumentException()
        }

        operandVariables.forEach { operandVariable ->
            flow = flow.addNotApprovedFact(
                expressionVariable, UnapprovedFirDataFlowInfo(
                    eq(conditionValue),
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

    override fun exitJump(jump: FirJump<*>) {
        graphBuilder.exitJump(jump).passFlow()
    }

    // ----------------------------------- When -----------------------------------

    override fun enterWhenExpression(whenExpression: FirWhenExpression) {
        graphBuilder.enterWhenExpression(whenExpression).passFlow()
    }

    override fun enterWhenBranchCondition(whenBranch: FirWhenBranch) {
        val node = graphBuilder.enterWhenBranchCondition(whenBranch).passFlow(false)
        val previousNode = node.previousNodes.single()
        if (previousNode is WhenBranchConditionExitNode) {
            node.flow = logicSystem.approveFactsInsideFlow(previousNode.falseCondition, node.flow)
        }
        node.flow.freeze()
    }

    override fun exitWhenBranchCondition(whenBranch: FirWhenBranch) {
        val node = graphBuilder.exitWhenBranchCondition(whenBranch).passFlow()

        val conditionVariable = getVariable(whenBranch.condition)
        node.trueCondition = conditionVariable eq True
        node.falseCondition = conditionVariable eq False
    }

    override fun exitWhenBranchResult(whenBranch: FirWhenBranch) {
        val node = graphBuilder.exitWhenBranchResult(whenBranch).passFlow(false)
        val conditionVariable = getVariable(whenBranch.condition)
        node.flow = node.flow.removeSyntheticVariable(conditionVariable)
    }

    override fun exitWhenExpression(whenExpression: FirWhenExpression) {
        val node = graphBuilder.exitWhenExpression(whenExpression)
        var flow = logicSystem.or(node.usefulPreviousNodes.map { it.flow })
        val subjectSymbol = whenExpression.subjectVariable?.symbol
        if (subjectSymbol != null) {
            variableStorage[subjectSymbol]?.let { flow = flow.removeVariable(it) }
        }
        node.flow = flow
    }

    // ----------------------------------- While Loop -----------------------------------

    override fun enterWhileLoop(loop: FirLoop) {
        graphBuilder.enterWhileLoop(loop).passFlow()
    }

    override fun exitWhileLoopCondition(loop: FirLoop) {
        graphBuilder.exitWhileLoopCondition(loop).passFlow()
    }

    override fun exitWhileLoop(loop: FirLoop) {
        graphBuilder.exitWhileLoop(loop).also { (blockExitNode, exitNode) ->
            blockExitNode.passFlow()
            exitNode.passFlow()
        }
    }

    // ----------------------------------- Do while Loop -----------------------------------

    override fun enterDoWhileLoop(loop: FirLoop) {
        graphBuilder.enterDoWhileLoop(loop).passFlow()
    }

    override fun enterDoWhileLoopCondition(loop: FirLoop) {
        val (loopBlockExitNode, loopConditionEnterNode) = graphBuilder.enterDoWhileLoopCondition(loop)
        loopBlockExitNode.passFlow()
        loopConditionEnterNode.passFlow()
    }

    override fun exitDoWhileLoop(loop: FirLoop) {
        graphBuilder.exitDoWhileLoop(loop).passFlow()
    }

    // ----------------------------------- Try-catch-finally -----------------------------------

    override fun enterTryExpression(tryExpression: FirTryExpression) {
        graphBuilder.enterTryExpression(tryExpression).passFlow()
    }

    override fun exitTryMainBlock(tryExpression: FirTryExpression) {
        graphBuilder.exitTryMainBlock(tryExpression).passFlow()
    }

    override fun enterCatchClause(catch: FirCatch) {
        graphBuilder.enterCatchClause(catch).passFlow()
    }

    override fun exitCatchClause(catch: FirCatch) {
        graphBuilder.exitCatchClause(catch).passFlow()
    }

    override fun enterFinallyBlock(tryExpression: FirTryExpression) {
        // TODO
        graphBuilder.enterFinallyBlock(tryExpression).passFlow()
    }

    override fun exitFinallyBlock(tryExpression: FirTryExpression) {
        // TODO
        graphBuilder.exitFinallyBlock(tryExpression).passFlow()
    }

    override fun exitTryExpression(tryExpression: FirTryExpression) {
        // TODO
        graphBuilder.exitTryExpression(tryExpression).passFlow()
    }

    // ----------------------------------- Resolvable call -----------------------------------

    override fun exitQualifiedAccessExpression(qualifiedAccessExpression: FirQualifiedAccessExpression) {
        graphBuilder.exitQualifiedAccessExpression(qualifiedAccessExpression).passFlow()
    }

    override fun enterFunctionCall(functionCall: FirFunctionCall) {
        // TODO: add processing in-place lambdas
    }

    override fun exitFunctionCall(functionCall: FirFunctionCall) {
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

    override fun exitConstExpresion(constExpression: FirConstExpression<*>) {
        graphBuilder.exitConstExpresion(constExpression).passFlow()
    }

    override fun exitVariableDeclaration(variable: FirVariable<*>) {
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

    override fun exitVariableAssignment(assignment: FirVariableAssignment) {
        graphBuilder.exitVariableAssignment(assignment).passFlow()
        val lhsVariable = variableStorage[assignment.resolvedSymbol ?: return] ?: return
        val rhsVariable = variableStorage[assignment.rValue.resolvedSymbol ?: return]?.takeIf { !it.isSynthetic } ?: return
        variableStorage.rebindAliasVariable(lhsVariable, rhsVariable)
    }

    override fun exitThrowExceptionNode(throwExpression: FirThrowExpression) {
        graphBuilder.exitThrowExceptionNode(throwExpression).passFlow()
    }

    // ----------------------------------- Boolean operators -----------------------------------

    override fun enterBinaryAnd(binaryLogicExpression: FirBinaryLogicExpression) {
        graphBuilder.enterBinaryAnd(binaryLogicExpression).passFlow()
    }

    override fun exitLeftBinaryAndArgument(binaryLogicExpression: FirBinaryLogicExpression) {
        val node = graphBuilder.exitLeftBinaryAndArgument(binaryLogicExpression).passFlow(false)
        val leftOperandVariable = getVariable(node.previousNodes.first().fir)
        node.flow = logicSystem.approveFactsInsideFlow(leftOperandVariable eq True, node.flow).also { it.freeze() }
    }

    override fun exitBinaryAnd(binaryLogicExpression: FirBinaryLogicExpression) {
        val node = graphBuilder.exitBinaryAnd(binaryLogicExpression).passFlow(false)
        val (leftVariable, rightVariable) = binaryLogicExpression.getVariables()

        val flowFromLeft = node.leftOperandNode.flow
        val flowFromRight = node.rightOperandNode.flow
        val flow = node.flow

        val andVariable = getVariable(binaryLogicExpression)

        val leftIsTrue = approveFact(leftVariable, True, flowFromRight)
        val leftIsFalse = approveFact(leftVariable, False, flowFromRight)
        val rightIsTrue = approveFact(rightVariable, True, flowFromRight) ?: mutableMapOf()
        val rightIsFalse = approveFact(rightVariable, False, flowFromRight)

        flowFromRight.approvedFacts.forEach { (variable, info) ->
            val actualInfo = flowFromLeft.approvedFacts[variable]?.let { info - it } ?: info
            if (actualInfo.isNotEmpty) rightIsTrue.compute(variable) { _, existingInfo -> info + existingInfo }
        }

        logicSystem.andForVerifiedFacts(leftIsTrue, rightIsTrue)?.let {
            for ((variable, info) in it) {
                flow.addNotApprovedFact(andVariable, UnapprovedFirDataFlowInfo(eq(True), variable, info))
            }
        }
        logicSystem.orForVerifiedFacts(leftIsFalse, rightIsFalse)?.let {
            for ((variable, info) in it) {
                flow.addNotApprovedFact(andVariable, UnapprovedFirDataFlowInfo(eq(False), variable, info))
            }
        }
        node.flow = flow.removeSyntheticVariable(leftVariable).removeSyntheticVariable(rightVariable).also { it.freeze() }
    }

    override fun enterBinaryOr(binaryLogicExpression: FirBinaryLogicExpression) {
        graphBuilder.enterBinaryOr(binaryLogicExpression)
    }

    override fun exitLeftBinaryOrArgument(binaryLogicExpression: FirBinaryLogicExpression) {
        val node = graphBuilder.exitLeftBinaryOrArgument(binaryLogicExpression).passFlow(false)
        val leftOperandVariable = getVariable(node.previousNodes.first().fir)
        node.flow = logicSystem.approveFactsInsideFlow(leftOperandVariable eq False, node.flow).also { it.freeze() }
    }

    override fun exitBinaryOr(binaryLogicExpression: FirBinaryLogicExpression) {
        val node = graphBuilder.exitBinaryOr(binaryLogicExpression).passFlow(false)
        val (leftVariable, rightVariable) = binaryLogicExpression.getVariables()

        val flowFromLeft = node.leftOperandNode.flow
        val flowFromRight = node.rightOperandNode.flow
        val flow = node.flow

        val orVariable = getVariable(binaryLogicExpression)

        val leftIsTrue = approveFact(leftVariable, True, flowFromLeft)
        val leftIsFalse = approveFact(leftVariable, False, flowFromLeft)
        val rightIsTrue = approveFact(rightVariable, True, flowFromRight)
        val rightIsFalse = approveFact(rightVariable, False, flowFromRight)

        logicSystem.orForVerifiedFacts(leftIsTrue, rightIsTrue)?.let {
            for ((variable, info) in it) {
                flow.addNotApprovedFact(orVariable, UnapprovedFirDataFlowInfo(eq(True), variable, info))
            }
        }
        logicSystem.andForVerifiedFacts(leftIsFalse, rightIsFalse)?.let {
            for ((variable, info) in it) {
                flow.addNotApprovedFact(orVariable, UnapprovedFirDataFlowInfo(eq(False), variable, info))
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

    override fun enterAnnotationCall(annotationCall: FirAnnotationCall) {
        graphBuilder.enterAnnotationCall(annotationCall).passFlow()
    }

    override fun exitAnnotationCall(annotationCall: FirAnnotationCall) {
        graphBuilder.exitAnnotationCall(annotationCall).passFlow()
    }

    // ----------------------------------- Init block -----------------------------------

    override fun enterInitBlock(initBlock: FirAnonymousInitializer) {
        graphBuilder.enterInitBlock(initBlock)
    }

    override fun exitInitBlock(initBlock: FirAnonymousInitializer) {
        graphBuilder.exitInitBlock(initBlock).passFlow()
    }

    // -------------------------------------------------------------------------------------------------------------------------

    private fun approveFact(variable: DataFlowVariable, value: ConditionValue, flow: Flow): MutableMap<DataFlowVariable, FirDataFlowInfo>? =
        logicSystem.approveFact(variable eq value, flow)

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