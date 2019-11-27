/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.dfa

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirSymbolOwner
import org.jetbrains.kotlin.fir.contracts.description.*
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.impl.FirThisReceiverExpressionImpl
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.references.impl.FirExplicitThisReference
import org.jetbrains.kotlin.fir.resolve.BodyResolveComponents
import org.jetbrains.kotlin.fir.resolve.ImplicitReceiverStackImpl
import org.jetbrains.kotlin.fir.resolve.ResolutionMode
import org.jetbrains.kotlin.fir.resolve.calls.FirNamedReferenceWithCandidate
import org.jetbrains.kotlin.fir.resolve.dfa.Condition.*
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.*
import org.jetbrains.kotlin.fir.resolve.dfa.contracts.buildContractFir
import org.jetbrains.kotlin.fir.resolve.dfa.contracts.createArgumentsMapping
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.FirAbstractBodyResolveTransformer
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.resultType
import org.jetbrains.kotlin.fir.resolve.withNullability
import org.jetbrains.kotlin.fir.resolvedTypeFromPrototype
import org.jetbrains.kotlin.fir.symbols.AbstractFirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.CallableId
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.visitors.transformSingle
import org.jetbrains.kotlin.ir.expressions.IrConstKind
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.utils.addIfNotNull
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

class FirDataFlowAnalyzer(private val components: FirAbstractBodyResolveTransformer.BodyResolveTransformerComponents) : BodyResolveComponents by components {
    companion object {
        private val KOTLIN_BOOLEAN_NOT = CallableId(FqName("kotlin"), FqName("Boolean"), Name.identifier("not"))
    }

    private val context: DataFlowInferenceContext get() = inferenceComponents.ctx as DataFlowInferenceContext
    private val receiverStack: ImplicitReceiverStackImpl = components.implicitReceiverStack as ImplicitReceiverStackImpl

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
        if (function.body == null) {
            node.mergeIncomingFlow()
        }
        for (valueParameter in function.valueParameters) {
            variableStorage.removeRealVariable(valueParameter.symbol)
        }
        if (graphBuilder.isTopLevel()) {
            flowOnNodes.clear()
            variableStorage.reset()
            graphBuilder.reset()
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
        graphBuilder.enterBlock(block)?.mergeIncomingFlow()
    }

    fun exitBlock(block: FirBlock) {
        graphBuilder.exitBlock(block).mergeIncomingFlow()
    }

    private fun FirElement.extractReturnType(target: FirFunction<*>?): ConeKotlinType? {
        return when (this) {
            is FirReturnExpression -> {
                if (this.target.labeledElement == target) {
                    result.extractReturnType(target)
                } else {
                    result.resultType.coneTypeSafe()
                }
            }
            is FirExpression -> {
                typeRef.coneTypeSafe()
            }
            else -> session.builtinTypes.unitType.type
        }
    }

    fun returnExpressionsOfAnonymousFunction(function: FirAnonymousFunction): List<FirStatement> {
        return graphBuilder.returnExpressionsOfAnonymousFunction(function)
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
        val (whenExitNode, syntheticElseNode) = graphBuilder.exitWhenExpression(whenExpression)
        if (syntheticElseNode != null) {
            syntheticElseNode.mergeIncomingFlow()
            val previousConditionExitNode = syntheticElseNode.previousNodes.single() as? WhenBranchConditionExitNode
            // previous node for syntheticElseNode can be not WhenBranchConditionExitNode in case of `when` without any branches
            // in that case there will be when enter or subject access node
            if (previousConditionExitNode != null) {
                syntheticElseNode.flow = logicSystem.approveFactsInsideFlow(
                    variablesForWhenConditions.remove(previousConditionExitNode)!!,
                    EqFalse,
                    syntheticElseNode.flow,
                    shouldForkFlow = true,
                    shouldRemoveSynthetics = true
                )
            }

        }
        val previousFlows = whenExitNode.alivePreviousNodes.map { it.flow }
        val flow = logicSystem.joinFlow(previousFlows)
        whenExitNode.flow = flow
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
        variableStorage[loop.condition]?.let { conditionVariable ->
            loopBlockEnterNode.flow = logicSystem.approveFactsInsideFlow(
                conditionVariable,
                EqTrue,
                loopBlockEnterNode.flow,
                shouldForkFlow = false,
                shouldRemoveSynthetics = true
            )
        }
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

    private fun enterSafeCall(qualifiedAccess: FirQualifiedAccess) {
        if (!qualifiedAccess.safe) return
        val node = graphBuilder.enterSafeCall(qualifiedAccess).mergeIncomingFlow()
        val previousNode = node.alivePreviousNodes.first()
        val shouldFork: Boolean
        var flow= if (previousNode is ExitSafeCallNode) {
            shouldFork = false
            previousNode.alivePreviousNodes.getOrNull(1)?.flow ?: node.flow
        } else {
            shouldFork = true
            node.flow
        }
        qualifiedAccess.explicitReceiver?.let {
            val type = it.typeRef.coneTypeSafe<ConeKotlinType>()
                ?.takeIf { it.isMarkedNullable }
                ?.withNullability(ConeNullability.NOT_NULL)
                ?: return@let

            when (val variable = getOrCreateVariable(it)) {
                is RealDataFlowVariable -> {
                    if (shouldFork) {
                        flow = logicSystem.forkFlow(flow)
                    }
                    logicSystem.addApprovedInfo(flow, variable, FirDataFlowInfo(setOf(type), emptySet()))
                }
                is SyntheticDataFlowVariable -> {
                    flow = logicSystem.approveFactsInsideFlow(variable, NotEqNull, flow, shouldFork, true)
                }
            }
        }

        node.flow = flow
    }

    private fun exitSafeCall(qualifiedAccess: FirQualifiedAccess) {
        if (!qualifiedAccess.safe) return
        graphBuilder.exitSafeCall(qualifiedAccess).mergeIncomingFlow()
    }

    fun enterQualifiedAccessExpression(qualifiedAccessExpression: FirQualifiedAccessExpression) {
        enterSafeCall(qualifiedAccessExpression)
    }

    fun exitQualifiedAccessExpression(qualifiedAccessExpression: FirQualifiedAccessExpression) {
        graphBuilder.exitQualifiedAccessExpression(qualifiedAccessExpression).mergeIncomingFlow()
        exitSafeCall(qualifiedAccessExpression)
    }

    fun exitFunctionCall(functionCall: FirFunctionCall) {
        val node = graphBuilder.exitFunctionCall(functionCall).mergeIncomingFlow()
        if (functionCall.isBooleanNot()) {
            exitBooleanNot(functionCall, node)
        }
        processConditionalContract(functionCall)
        if (functionCall.safe) {
            exitSafeCall(functionCall)
        }
    }

    private fun processConditionalContract(functionCall: FirFunctionCall) {
        val contractDescription = (functionCall.resolvedSymbol?.fir as? FirSimpleFunction)?.contractDescription ?: return
        val conditionalEffects = contractDescription.effects.filterIsInstance<ConeConditionalEffectDeclaration>()
        if (conditionalEffects.isEmpty()) return
        val argumentsMapping = createArgumentsMapping(functionCall) ?: return
        graphBuilder.enterContract(functionCall).mergeIncomingFlow()
        val functionCallVariable = getOrCreateVariable(functionCall)
        for (conditionalEffect in conditionalEffects) {
            val fir = conditionalEffect.buildContractFir(argumentsMapping) ?: continue
            val effect = conditionalEffect.effect as? ConeReturnsEffectDeclaration ?: continue
            fir.transformSingle(components.transformer, ResolutionMode.ContextDependent)
            val argumentVariable = getOrCreateVariable(fir)
            val lastNode = graphBuilder.lastNode
            when (val value = effect.value) {
                ConeConstantReference.WILDCARD -> {
                    lastNode.flow = logicSystem.approveFactsInsideFlow(
                        argumentVariable,
                        EqTrue,
                        lastNode.flow,
                        shouldForkFlow = false,
                        shouldRemoveSynthetics = true
                    )
                }

                ConeBooleanConstantReference.TRUE, ConeBooleanConstantReference.FALSE -> {
                    logicSystem.changeVariableForConditionFlow(lastNode.flow, argumentVariable, functionCallVariable) {
                        it.takeIf { it.condition == if (value == ConeBooleanConstantReference.TRUE) EqTrue else EqFalse }
                    }
                }

                ConeConstantReference.NOT_NULL, ConeConstantReference.NULL -> {
                    logicSystem.changeVariableForConditionFlow(lastNode.flow, argumentVariable, functionCallVariable) {
                        it.takeIf { it.condition == EqTrue }?.let {
                            val condition = if (value == ConeConstantReference.NOT_NULL) Condition.NotEqNull else NotEqNull
                            ConditionalFirDataFlowInfo(condition, it.variable, it.info)
                        }
                    }
                }

                else -> throw IllegalArgumentException(value.toString())
            }
        }
        graphBuilder.exitContract(functionCall).mergeIncomingFlow()
    }


    private val FirElement.resolvedSymbol: AbstractFirBasedSymbol<*>?
        get() {
            return when (this) {
                is FirResolvable -> resolvedSymbol
                is FirSymbolOwner<*> -> symbol
                else -> null
            }
        }

    private val FirResolvable.resolvedSymbol: AbstractFirBasedSymbol<*>?
        get() = calleeReference.let {
            when (it) {
                is FirExplicitThisReference -> it.boundSymbol
                is FirResolvedNamedReference -> it.resolvedSymbol
                is FirNamedReferenceWithCandidate -> it.candidateSymbol
                else -> null
            }
        }

    private fun FirFunctionCall.isBooleanNot(): Boolean {
        val symbol = calleeReference.safeAs<FirResolvedNamedReference>()?.resolvedSymbol as? FirNamedFunctionSymbol ?: return false
        return symbol.callableId == KOTLIN_BOOLEAN_NOT
    }

    fun exitConstExpresion(constExpression: FirConstExpression<*>) {
        graphBuilder.exitConstExpresion(constExpression).mergeIncomingFlow()
    }

    fun exitVariableDeclaration(variable: FirProperty) {
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
        variableStorage[initializer]?.takeIf { it.isSynthetic() }?.let { initializerVariable ->
            val realVariable = getOrCreateRealVariable(variable)
            requireNotNull(realVariable)
            logicSystem.changeVariableForConditionFlow(node.flow, initializerVariable, realVariable)
        }


        getOrCreateRealVariable(initializer)?.let { rhsVariable ->
            variableStorage.createAliasVariable(variable.symbol, rhsVariable)
        }
    }

    fun exitVariableAssignment(assignment: FirVariableAssignment) {
        val node = graphBuilder.exitVariableAssignment(assignment).mergeIncomingFlow()
        val lhsSymbol: AbstractFirBasedSymbol<*> = (assignment.lValue as? FirResolvedNamedReference)?.resolvedSymbol ?: return
        val lhsVariable = getOrCreateRealVariable(lhsSymbol.fir) ?: return
        val rhsSymbol = assignment.rValue.resolvedSymbol
        val rhsVariable = rhsSymbol?.let { variableStorage[it]?.takeIf { !it.isSynthetic() } }
        if (rhsVariable == null) {
            val type = assignment.rValue.typeRef.coneTypeSafe<ConeKotlinType>() ?: return
            logicSystem.addApprovedInfo(
                node.flow,
                lhsVariable,
                FirDataFlowInfo(setOf(type), emptySet())
            )
            return
        } else {
            variableStorage.rebindAliasVariable(lhsVariable, rhsVariable)
        }
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

    private fun FirElement.unwrapWhenSubjectExpression(): FirElement = if (this is FirWhenSubjectExpression) {
        val whenExpression = whenSubject.whenExpression
        whenExpression.subjectVariable
            ?: whenExpression.subject
            ?: throw IllegalStateException("Subject or subject variable must be not null")
    } else {
        this
    }

    private fun getOrCreateRealVariable(fir: FirElement): RealDataFlowVariable? {
        @Suppress("NAME_SHADOWING")
        val fir = fir.unwrapWhenSubjectExpression()
        if (fir is FirThisReceiverExpressionImpl) {
            return variableStorage.getOrCreateNewThisRealVariable(fir.calleeReference.boundSymbol ?: return null)
        }
        val symbol: AbstractFirBasedSymbol<*> = fir.resolvedSymbol ?: return null
        return variableStorage.getOrCreateNewRealVariable(symbol).variableUnderAlias
    }

    private fun getOrCreateVariable(fir: FirElement): DataFlowVariable {
        return getOrCreateRealVariable(fir) ?: getOrCreateSyntheticVariable(fir)
    }

    // -------------------------------- get variable --------------------------------

    private val FirElement.realVariable: RealDataFlowVariable?
        get() {
            val symbol: AbstractFirBasedSymbol<*> = if (this is FirThisReceiverExpression) {
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
                    result.addIfNotNull(getOrCreateRealVariable(call))
                }
                is FirWhenSubjectExpression -> {
                    // TODO: check
                    call.whenSubject.whenExpression.subjectVariable?.let { result += getOrCreateRealVariable(it)!! }
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
