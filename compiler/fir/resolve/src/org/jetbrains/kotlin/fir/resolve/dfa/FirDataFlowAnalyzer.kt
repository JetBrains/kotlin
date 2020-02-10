/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.dfa

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.contracts.description.ConeBooleanConstantReference
import org.jetbrains.kotlin.fir.contracts.description.ConeConditionalEffectDeclaration
import org.jetbrains.kotlin.fir.contracts.description.ConeConstantReference
import org.jetbrains.kotlin.fir.contracts.description.ConeReturnsEffectDeclaration
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.resolve.ImplicitReceiverStackImpl
import org.jetbrains.kotlin.fir.resolve.ResolutionMode
import org.jetbrains.kotlin.fir.resolve.calls.ConeInferenceContext
import org.jetbrains.kotlin.fir.resolve.defaultType
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.*
import org.jetbrains.kotlin.fir.resolve.dfa.contracts.buildContractFir
import org.jetbrains.kotlin.fir.resolve.dfa.contracts.createArgumentsMapping
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.FirAbstractBodyResolveTransformer
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.resultType
import org.jetbrains.kotlin.fir.resolve.withNullability
import org.jetbrains.kotlin.fir.symbols.AbstractFirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.CallableId
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.visitors.transformSingle
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

@UseExperimental(DfaInternals::class)
abstract class FirDataFlowAnalyzer<FLOW : Flow>(
    protected val components: FirAbstractBodyResolveTransformer.BodyResolveTransformerComponents
) {
    companion object {
        internal val KOTLIN_BOOLEAN_NOT = CallableId(FqName("kotlin"), FqName("Boolean"), Name.identifier("not"))

        fun createFirDataFlowAnalyzer(
            components: FirAbstractBodyResolveTransformer.BodyResolveTransformerComponents
        ): FirDataFlowAnalyzer<*> = object : FirDataFlowAnalyzer<PersistentFlow>(components) {
            private val receiverStack: ImplicitReceiverStackImpl = components.implicitReceiverStack as ImplicitReceiverStackImpl

            override val logicSystem: PersistentLogicSystem = object : PersistentLogicSystem(components.inferenceComponents.ctx) {
                override fun processUpdatedReceiverVariable(flow: PersistentFlow, variable: RealVariable) {
                    val symbol = variable.identifier.symbol

                    val index = receiverStack.getReceiverIndex(symbol) ?: return
                    val info = flow.getTypeStatement(variable)

                    if (info == null) {
                        receiverStack.replaceReceiverType(index, receiverStack.getOriginalType(index))
                    } else {
                        val types = info.exactType.toMutableList().also {
                            it += receiverStack.getOriginalType(index)
                        }
                        receiverStack.replaceReceiverType(index, context.intersectTypesOrNull(types)!!)
                    }
                }

                override fun updateAllReceivers(flow: PersistentFlow) {
                    receiverStack.forEach {
                        variableStorage.getRealVariable(it.boundSymbol, it.receiverExpression, flow)?.let { variable ->
                            processUpdatedReceiverVariable(flow, variable)
                        }
                    }
                }
            }
        }
    }

    protected abstract val logicSystem: LogicSystem<FLOW>
    private val context: ConeInferenceContext = components.inferenceComponents.ctx

    private val graphBuilder = ControlFlowGraphBuilder()
    protected val variableStorage: VariableStorage = VariableStorage(components.session)
    private val flowOnNodes = mutableMapOf<CFGNode<*>, FLOW>()

    private val variablesForWhenConditions = mutableMapOf<WhenBranchConditionExitNode, DataFlowVariable>()

    private var contractDescriptionVisitingMode = false

    protected val any = components.session.builtinTypes.anyType.coneTypeUnsafe<ConeKotlinType>()
    private val nullableNothing = components.session.builtinTypes.nullableNothingType.coneTypeUnsafe<ConeKotlinType>()

    // ----------------------------------- Requests -----------------------------------

    fun getTypeUsingSmartcastInfo(qualifiedAccessExpression: FirQualifiedAccessExpression): MutableList<ConeKotlinType>? {
        /*
         * DataFlowAnalyzer holds variables only for declarations that have some smartcast (or can have)
         * If there is no useful information there is no data flow variable also
         */
        val symbol: AbstractFirBasedSymbol<*> = qualifiedAccessExpression.symbol ?: return null
        val flow = graphBuilder.lastNode.flow
        var variable = variableStorage.getRealVariableWithoutUnwrappingAlias(symbol, qualifiedAccessExpression, flow) ?: return null
        val result = mutableListOf<ConeKotlinType>()
        flow.directAliasMap[variable]?.let {
            result += it.originalType
            variable = it
        }
        flow.getTypeStatement(variable)?.exactType?.let { result += it }
        return result.takeIf { it.isNotEmpty() }
    }

    fun returnExpressionsOfAnonymousFunction(function: FirAnonymousFunction): List<FirStatement> {
        return graphBuilder.returnExpressionsOfAnonymousFunction(function)
    }

    // ----------------------------------- Named function -----------------------------------

    fun enterFunction(function: FirFunction<*>) {
        val (functionEnterNode, previousNode) = graphBuilder.enterFunction(function)
        if (previousNode == null) {
            functionEnterNode.mergeIncomingFlow()
        } else {
            // Enter anonymous function
            assert(functionEnterNode.previousNodes.isEmpty())
            functionEnterNode.flow = logicSystem.forkFlow(previousNode.flow)
        }
    }

    fun exitFunction(function: FirFunction<*>): ControlFlowGraph? {
        val (node, graph) = graphBuilder.exitFunction(function)
        if (function.body == null) {
            node.mergeIncomingFlow()
        }
        if (!graphBuilder.isTopLevel()) {
            for (valueParameter in function.valueParameters) {
                variableStorage.removeRealVariable(valueParameter.symbol)
            }
        }
        if (graphBuilder.isTopLevel()) {
            flowOnNodes.clear()
            variableStorage.reset()
            graphBuilder.reset()
        }
        return graph
    }

    // ----------------------------------- Anonymous function -----------------------------------

    fun visitPostponedAnonymousFunction(anonymousFunction: FirAnonymousFunction) {
        val (enterNode, exitNode) = graphBuilder.visitPostponedAnonymousFunction(anonymousFunction)
        enterNode.mergeIncomingFlow()
        exitNode.mergeIncomingFlow()
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

    // ----------------------------------- Operator call -----------------------------------

    fun exitTypeOperatorCall(typeOperatorCall: FirTypeOperatorCall) {
        val node = graphBuilder.exitTypeOperatorCall(typeOperatorCall).mergeIncomingFlow()
        if (typeOperatorCall.operation !in FirOperation.TYPES) return
        val type = typeOperatorCall.conversionTypeRef.coneTypeUnsafe<ConeKotlinType>()
        val operandVariable = variableStorage.getOrCreateVariable(node.previousFlow, typeOperatorCall.argument)
        val flow = node.flow

        when (val operation = typeOperatorCall.operation) {
            FirOperation.IS, FirOperation.NOT_IS -> {
                val expressionVariable = variableStorage.createSyntheticVariable(typeOperatorCall)
                val isNotNullCheck = operation == FirOperation.IS && type.nullability == ConeNullability.NOT_NULL
                if (operandVariable.isReal()) {
                    val hasTypeInfo = operandVariable typeEq type
                    val hasNotTypeInfo = operandVariable typeNotEq type

                    fun chooseInfo(trueBranch: Boolean) =
                        if ((typeOperatorCall.operation == FirOperation.IS) == trueBranch) hasTypeInfo else hasNotTypeInfo

                    flow.addImplication((expressionVariable eq true) implies chooseInfo(true))
                    flow.addImplication((expressionVariable eq false) implies chooseInfo(false))

                    if (operation == FirOperation.NOT_IS && type == nullableNothing) {
                        flow.addTypeStatement(operandVariable typeEq any)
                    }
                    if (isNotNullCheck) {
                        flow.addImplication((expressionVariable eq true) implies (operandVariable typeEq any))
                        flow.addImplication((expressionVariable eq true) implies (operandVariable notEq null))
                    }

                } else {
                    if (isNotNullCheck) {
                        flow.addImplication((expressionVariable eq true) implies (operandVariable notEq null))
                    }
                }
            }

            FirOperation.AS -> {
                if (operandVariable.isReal()) {
                    flow.addTypeStatement(operandVariable typeEq type)
                }
                logicSystem.approveStatementsInsideFlow(
                    flow,
                    operandVariable notEq null,
                    shouldRemoveSynthetics = true,
                    shouldForkFlow = false
                )
            }

            FirOperation.SAFE_AS -> {
                val expressionVariable = variableStorage.createSyntheticVariable(typeOperatorCall)
                if (operandVariable.isReal()) {
                    flow.addImplication((expressionVariable notEq null) implies (operandVariable typeEq type))
                    flow.addImplication((expressionVariable eq null) implies (operandVariable typeNotEq type))
                }
                if (type.nullability == ConeNullability.NOT_NULL) {
                    flow.addImplication((expressionVariable notEq null) implies (operandVariable notEq null))
                }
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
                    leftConst != null && rightConst != null -> return
                    leftConst?.kind == FirConstKind.Null -> processEqNull(node, rightOperand, operation)
                    rightConst?.kind == FirConstKind.Null -> processEqNull(node, leftOperand, operation)
                    leftConst != null -> processEqWithConst(node, rightOperand, leftConst, operation)
                    rightConst != null -> processEqWithConst(node, leftOperand, rightConst, operation)
                    else -> processEq(node, leftOperand, rightOperand, operation)
                }
            }
        }
    }

    // const != null
    private fun processEqWithConst(node: OperatorCallNode, operand: FirExpression, const: FirConstExpression<*>, operation: FirOperation) {
        val isEq = operation.isEq()
        val expressionVariable = variableStorage.createSyntheticVariable(node.fir)
        val flow = node.flow
        val operandVariable = variableStorage.getOrCreateVariable(node.previousFlow, operand)
        // expression == const -> expression != null
        flow.addImplication((expressionVariable eq isEq) implies (operandVariable notEq null))
        if (operandVariable is RealVariable) {
            flow.addImplication((expressionVariable eq isEq) implies (operandVariable typeEq any))
        }

        // propagating facts for (... == true) and (... == false)
        if (const.kind == FirConstKind.Boolean) {
            val constValue = const.value as Boolean
            val shouldInvert = isEq xor constValue

            logicSystem.translateVariableFromConditionInStatements(
                flow,
                operandVariable,
                expressionVariable,
                shouldRemoveOriginalStatements = operandVariable.isSynthetic()
            ) {
                when (it.condition.operation) {
                    Operation.EqNull, Operation.NotEqNull -> {
                        (expressionVariable eq isEq) implies (it.effect)
                    }
                    Operation.EqTrue, Operation.EqFalse -> {
                        if (shouldInvert) (it.condition.invert()) implies (it.effect)
                        else it
                    }
                }
            }
        }
    }

    private fun processEq(node: OperatorCallNode, leftOperand: FirExpression, rightOperand: FirExpression, operation: FirOperation) {
        val leftIsNullable = leftOperand.coneType?.isMarkedNullable ?: return
        val rightIsNullable = rightOperand.coneType?.isMarkedNullable ?: return
        // left == right && right not null -> left != null
        when {
            leftIsNullable && rightIsNullable -> return
            leftIsNullable -> processEqNull(node, leftOperand, operation.invert())
            rightIsNullable -> processEqNull(node, rightOperand, operation.invert())
        }
    }

    private fun processEqNull(node: OperatorCallNode, operand: FirExpression, operation: FirOperation) {
        val flow = node.flow
        val expressionVariable = variableStorage.createSyntheticVariable(node.fir)
        val operandVariable = variableStorage.getOrCreateVariable(node.previousFlow, operand)

        val isEq = operation.isEq()

        val predicate = when (isEq) {
            true -> operandVariable eq null
            false -> operandVariable notEq null
        }

        logicSystem.approveOperationStatement(flow, predicate).forEach { effect ->
            flow.addImplication((expressionVariable eq true) implies effect)
            flow.addImplication((expressionVariable eq false) implies effect.invert())
        }

        flow.addImplication((expressionVariable eq isEq) implies (operandVariable eq null))
        flow.addImplication((expressionVariable notEq isEq) implies (operandVariable notEq null))

        if (operandVariable is RealVariable) {
            flow.addImplication((expressionVariable eq isEq) implies (operandVariable typeNotEq any))
            flow.addImplication((expressionVariable notEq isEq) implies (operandVariable typeEq any))

//            TODO: design do we need casts to Nothing?
//            flow.addImplication((expressionVariable eq !isEq) implies (operandVariable typeEq nullableNothing))
//            flow.addImplication((expressionVariable notEq !isEq) implies (operandVariable typeNotEq nullableNothing))
        }
        node.flow = flow
    }

    // ----------------------------------- Jump -----------------------------------

    fun exitJump(jump: FirJump<*>) {
        graphBuilder.exitJump(jump).mergeIncomingFlow()
    }

    // ----------------------------------- Check not null call -----------------------------------

    fun exitCheckNotNullCall(checkNotNullCall: FirCheckNotNullCall) {
        // Add `Any` to the set of possible types; the intersection type `T? & Any` will be reduced to `T` after smartcast.
        val node = graphBuilder.exitCheckNotNullCall(checkNotNullCall).mergeIncomingFlow()
        val argument = checkNotNullCall.argument
        val operandVariable = variableStorage.getOrCreateRealVariable(node.previousFlow, argument.symbol, argument) ?: return
        node.flow.addTypeStatement(operandVariable typeEq any)
        logicSystem.approveStatementsInsideFlow(
            node.flow,
            operandVariable notEq null,
            shouldRemoveSynthetics = true,
            shouldForkFlow = false
        )
    }

    // ----------------------------------- When -----------------------------------

    fun enterWhenExpression(whenExpression: FirWhenExpression) {
        graphBuilder.enterWhenExpression(whenExpression).mergeIncomingFlow()
    }

    fun enterWhenBranchCondition(whenBranch: FirWhenBranch) {
        val node = graphBuilder.enterWhenBranchCondition(whenBranch).mergeIncomingFlow(updateReceivers = true)
        val previousNode = node.previousNodes.single()
        if (previousNode is WhenBranchConditionExitNode) {
            val conditionVariable = variablesForWhenConditions.remove(previousNode)!!
            node.flow = logicSystem.approveStatementsInsideFlow(
                node.flow,
                conditionVariable eq false,
                shouldForkFlow = true,
                shouldRemoveSynthetics = true
            )
        }
    }

    fun exitWhenBranchCondition(whenBranch: FirWhenBranch) {
        val (conditionExitNode, branchEnterNode) = graphBuilder.exitWhenBranchCondition(whenBranch)
        conditionExitNode.mergeIncomingFlow()

        val conditionExitFlow = conditionExitNode.flow
        val conditionVariable = variableStorage.getOrCreateVariable(conditionExitFlow, whenBranch.condition)
        variablesForWhenConditions[conditionExitNode] = conditionVariable
        branchEnterNode.flow = logicSystem.approveStatementsInsideFlow(
            conditionExitFlow,
            conditionVariable eq true,
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
            val previousConditionExitNode = syntheticElseNode.firstPreviousNode as? WhenBranchConditionExitNode
            // previous node for syntheticElseNode can be not WhenBranchConditionExitNode in case of `when` without any branches
            // in that case there will be when enter or subject access node
            if (previousConditionExitNode != null) {
                val conditionVariable = variablesForWhenConditions.remove(previousConditionExitNode)!!
                syntheticElseNode.flow = logicSystem.approveStatementsInsideFlow(
                    previousConditionExitNode.flow,
                    conditionVariable eq false,
                    shouldForkFlow = true,
                    shouldRemoveSynthetics = true
                )
            } else {
                syntheticElseNode.mergeIncomingFlow()
            }
        }
        whenExitNode.mergeIncomingFlow(updateReceivers = true)
    }

    // ----------------------------------- While Loop -----------------------------------

    private fun exitCommonLoop(exitNode: LoopExitNode) {
        val singlePreviousNode = exitNode.previousNodes.singleOrNull { !it.isDead }
        if (singlePreviousNode is LoopConditionExitNode) {
            val variable = variableStorage.getOrCreateVariable(exitNode.previousFlow, singlePreviousNode.fir)
            exitNode.flow = logicSystem.approveStatementsInsideFlow(
                exitNode.flow,
                variable eq false,
                shouldForkFlow = false,
                shouldRemoveSynthetics = true
            )
        }
    }

    fun enterWhileLoop(loop: FirLoop) {
        val (loopEnterNode, loopConditionEnterNode) = graphBuilder.enterWhileLoop(loop)
        loopEnterNode.mergeIncomingFlow()
        loopConditionEnterNode.mergeIncomingFlow()
    }

    fun exitWhileLoopCondition(loop: FirLoop) {
        val (loopConditionExitNode, loopBlockEnterNode) = graphBuilder.exitWhileLoopCondition(loop)
        loopConditionExitNode.mergeIncomingFlow()
        val conditionExitFlow = loopConditionExitNode.flow
        loopBlockEnterNode.flow = variableStorage.getVariable(loop.condition, conditionExitFlow)?.let { conditionVariable ->
            logicSystem.approveStatementsInsideFlow(
                conditionExitFlow,
                conditionVariable eq true,
                shouldForkFlow = true,
                shouldRemoveSynthetics = false
            )
        } ?: logicSystem.forkFlow(conditionExitFlow)
    }

    fun exitWhileLoop(loop: FirLoop) {
        val (blockExitNode, exitNode) = graphBuilder.exitWhileLoop(loop)
        blockExitNode.mergeIncomingFlow()
        exitNode.mergeIncomingFlow()
        exitCommonLoop(exitNode)
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
        exitCommonLoop(loopExitNode)
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
        graphBuilder.enterCatchClause(catch).mergeIncomingFlow(updateReceivers = true)
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

    fun enterQualifiedAccessExpression(qualifiedAccessExpression: FirQualifiedAccessExpression) {
        enterSafeCall(qualifiedAccessExpression)
    }

    private fun enterSafeCall(qualifiedAccess: FirQualifiedAccess) {
        if (!qualifiedAccess.safe) return
        val node = graphBuilder.enterSafeCall(qualifiedAccess).mergeIncomingFlow()
        val previousNode = node.firstPreviousNode
        val shouldFork: Boolean
        var flow = if (previousNode is ExitSafeCallNode) {
            shouldFork = false
            previousNode.secondPreviousNode?.flow ?: node.flow
        } else {
            shouldFork = true
            node.flow
        }
        qualifiedAccess.explicitReceiver?.let { receiver ->
            val type = receiver.coneType
                ?.takeIf { it.isMarkedNullable }
                ?.withNullability(ConeNullability.NOT_NULL)
                ?: return@let

            val variable = variableStorage.getOrCreateVariable(flow, receiver)
            if (variable is RealVariable) {
                if (shouldFork) {
                    flow = logicSystem.forkFlow(flow)
                }
                flow.addTypeStatement(variable typeEq type)
            }
            flow = logicSystem.approveStatementsInsideFlow(
                flow,
                variable notEq null,
                shouldFork,
                shouldRemoveSynthetics = true
            )
        }

        node.flow = flow
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

    private fun exitSafeCall(qualifiedAccess: FirQualifiedAccess) {
        if (!qualifiedAccess.safe) return
        val node = graphBuilder.exitSafeCall(qualifiedAccess).mergeIncomingFlow()
        val previousFlow = node.previousFlow
        val variable = variableStorage.getOrCreateVariable(previousFlow, qualifiedAccess)
        val receiverVariable = when (variable) {
            // There is some bug with invokes. See KT-36014
            is RealVariable -> variable.explicitReceiverVariable ?: return
            is SyntheticVariable -> variableStorage.getOrCreateVariable(previousFlow, qualifiedAccess.explicitReceiver!!)
        }
        logicSystem.addImplication(node.flow, (variable notEq null) implies (receiverVariable notEq null))
        if (receiverVariable.isReal()) {
            logicSystem.addImplication(node.flow, (variable notEq null) implies (receiverVariable typeEq any))
        }
    }

    private fun processConditionalContract(functionCall: FirFunctionCall) {
        val contractDescription = (functionCall.symbol as? FirNamedFunctionSymbol)?.fir?.contractDescription ?: return
        val conditionalEffects = contractDescription.effects.filterIsInstance<ConeConditionalEffectDeclaration>()
        if (conditionalEffects.isEmpty()) return
        val argumentsMapping = createArgumentsMapping(functionCall) ?: return
        contractDescriptionVisitingMode = true
        graphBuilder.enterContract(functionCall).mergeIncomingFlow()
        val lastFlow = graphBuilder.lastNode.flow
        val functionCallVariable = variableStorage.getOrCreateVariable(lastFlow, functionCall)
        for (conditionalEffect in conditionalEffects) {
            val fir = conditionalEffect.buildContractFir(argumentsMapping) ?: continue
            val effect = conditionalEffect.effect as? ConeReturnsEffectDeclaration ?: continue
            fir.transformSingle(components.transformer, ResolutionMode.ContextDependent)
            val argumentVariable = variableStorage.getOrCreateVariable(lastFlow, fir)
            val lastNode = graphBuilder.lastNode
            when (val value = effect.value) {
                ConeConstantReference.WILDCARD -> {
                    lastNode.flow = logicSystem.approveStatementsInsideFlow(
                        lastNode.flow,
                        argumentVariable eq true,
                        shouldForkFlow = false,
                        shouldRemoveSynthetics = true
                    )
                }

                is ConeBooleanConstantReference -> {
                    logicSystem.replaceVariableFromConditionInStatements(
                        lastNode.flow,
                        argumentVariable,
                        functionCallVariable,
                        filter = { it.condition.operation == value.toOperation() }
                    )
                }

                ConeConstantReference.NOT_NULL, ConeConstantReference.NULL -> {
                    logicSystem.replaceVariableFromConditionInStatements(
                        lastNode.flow,
                        argumentVariable,
                        functionCallVariable,
                        filter = { it.condition.operation == Operation.EqTrue },
                        transform = { OperationStatement(it.condition.variable, value.toOperation()) implies it.effect }
                    )
                }

                else -> throw IllegalArgumentException("Unsupported constant reference: $value")
            }
        }
        graphBuilder.exitContract(functionCall).mergeIncomingFlow()
        contractDescriptionVisitingMode = true
    }

    fun exitConstExpresion(constExpression: FirConstExpression<*>) {
        if (constExpression.resultType is FirResolvedTypeRef && !contractDescriptionVisitingMode) return
        graphBuilder.exitConstExpresion(constExpression).mergeIncomingFlow()
    }

    fun exitLocalVariableDeclaration(variable: FirProperty) {
        val node = graphBuilder.exitVariableDeclaration(variable).mergeIncomingFlow()
        val initializer = variable.initializer ?: return
        exitVariableInitialization(node, initializer, variable, assignment = null)
    }

    fun exitVariableAssignment(assignment: FirVariableAssignment) {
        val node = graphBuilder.exitVariableAssignment(assignment).mergeIncomingFlow()
        val property = (assignment.lValue as? FirResolvedNamedReference)?.resolvedSymbol?.fir as? FirProperty ?: return
        // TODO: add unstable smartcast
        if (!property.isLocal && property.isVar) return
        exitVariableInitialization(node, assignment.rValue, property, assignment)
    }

    private fun exitVariableInitialization(node: CFGNode<*>, initializer: FirExpression, property: FirProperty, assignment: FirVariableAssignment?) {
        val flow = node.flow
        val propertyVariable = variableStorage.getOrCreateRealVariableWithoutUnwrappingAlias(flow, property.symbol, assignment ?: property)
        val isAssignment = assignment != null
        if (isAssignment) {
            logicSystem.removeLocalVariableAlias(flow, propertyVariable)
            flow.removeAllAboutVariable(propertyVariable)
        }

        variableStorage.getOrCreateRealVariable(flow, initializer.symbol, initializer)?.let { initializerVariable ->
            logicSystem.addLocalVariableAlias(flow, propertyVariable, initializerVariable)
            // node.flow.addImplication((propertyVariable notEq null) implies (initializerVariable notEq null))
        }

        variableStorage.getSyntheticVariable(initializer)?.let { initializerVariable ->
            /*
                 * That part is needed for cases like that:
                 *
                 *   val b = x is String
                 *   ...
                 *   if (b) {
                 *      x.length
                 *   }
                 */
            logicSystem.replaceVariableFromConditionInStatements(flow, initializerVariable, propertyVariable)
        }

        if (isAssignment) {
            if (initializer is FirConstExpression<*> && initializer.kind == FirConstKind.Null) return
            flow.addTypeStatement(propertyVariable typeEq initializer.typeRef.coneTypeUnsafe<ConeKotlinType>())
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
        val parentFlow = leftNode.firstPreviousNode.flow
        leftNode.flow = logicSystem.forkFlow(parentFlow)
        val leftOperandVariable = variableStorage.getOrCreateVariable(parentFlow, leftNode.firstPreviousNode.fir)
        rightNode.flow = logicSystem.approveStatementsInsideFlow(
            parentFlow,
            leftOperandVariable eq isAnd,
            shouldForkFlow = true,
            shouldRemoveSynthetics = false
        )
    }

    private fun exitBinaryBooleanOperator(
        binaryLogicExpression: FirBinaryLogicExpression,
        node: AbstractBinaryExitNode<*>,
        isAnd: Boolean
    ) {
        val bothEvaluated = isAnd
        val onlyLeftEvaluated = !bothEvaluated

        // Naming for all variables was chosen in assumption that we processing && expression
        val flowFromLeft = node.leftOperandNode.flow
        val flowFromRight = node.rightOperandNode.flow

        val flow = node.mergeIncomingFlow().flow

        /*
         * TODO: Here we should handle case when one of arguments is dead (e.g. in cases `false && expr` or `true || expr`)
         *  But since conditions with const are rare it can be delayed
         */

        val leftVariable = variableStorage.getOrCreateVariable(flow, binaryLogicExpression.leftOperand)
        val rightVariable = variableStorage.getOrCreateVariable(flow, binaryLogicExpression.rightOperand)
        val operatorVariable = variableStorage.getOrCreateVariable(flow, binaryLogicExpression)

        val (conditionalFromLeft, conditionalFromRight, approvedFromRight) = logicSystem.collectInfoForBooleanOperator(
            flowFromLeft,
            leftVariable,
            flowFromRight,
            rightVariable
        )

        // left && right == True
        // left || right == False
        val approvedIfTrue: MutableTypeStatements = mutableMapOf()
        logicSystem.approveStatementsTo(approvedIfTrue, flowFromRight, leftVariable eq bothEvaluated, conditionalFromLeft)
        logicSystem.approveStatementsTo(approvedIfTrue, flowFromRight, rightVariable eq bothEvaluated, conditionalFromRight)
        approvedFromRight.forEach { (variable, info) ->
            approvedIfTrue.addStatement(variable, info)
        }
        approvedIfTrue.values.forEach { info ->
            flow.addImplication((operatorVariable eq bothEvaluated) implies info)
        }

        // left && right == False
        // left || right == True
        val approvedIfFalse: MutableTypeStatements = mutableMapOf()
        val leftIsFalse = logicSystem.approveOperationStatement(flowFromLeft, leftVariable eq onlyLeftEvaluated, conditionalFromLeft)
        val rightIsFalse = logicSystem.approveOperationStatement(flowFromRight, rightVariable eq onlyLeftEvaluated, conditionalFromRight)
        approvedIfFalse.mergeTypeStatements(logicSystem.orForTypeStatements(leftIsFalse, rightIsFalse))
        approvedIfFalse.values.forEach { info ->
            flow.addImplication((operatorVariable eq onlyLeftEvaluated) implies info)
        }

        logicSystem.updateAllReceivers(flow)
        node.flow = flow

        variableStorage.removeSyntheticVariable(leftVariable)
        variableStorage.removeSyntheticVariable(rightVariable)
    }


    private fun exitBooleanNot(functionCall: FirFunctionCall, node: FunctionCallNode) {
        val previousFlow = node.previousFlow
        val booleanExpressionVariable = variableStorage.getOrCreateVariable(previousFlow, node.firstPreviousNode.fir)
        val variable = variableStorage.getOrCreateVariable(previousFlow, functionCall)
        logicSystem.replaceVariableFromConditionInStatements(
            node.flow,
            booleanExpressionVariable,
            variable,
            transform = { it.invertCondition() }
        )
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

    // ------------------------------------------------------ Utils ------------------------------------------------------

    private var CFGNode<*>.flow: FLOW
        get() = flowOnNodes.getValue(this.origin)
        set(value) {
            flowOnNodes[this.origin] = value
        }

    private val CFGNode<*>.origin: CFGNode<*> get() = if (this is StubNode) firstPreviousNode else this

    private fun <T : CFGNode<*>> T.mergeIncomingFlow(updateReceivers: Boolean = false): T = this.also { node ->
        val previousFlows = if (node.isDead)
            node.previousNodes.map { it.flow }
        else
            node.previousNodes.mapNotNull { prev -> prev.takeIf { node.incomingEdges.getValue(it).usedInDfa }?.flow }
        val flow = logicSystem.joinFlow(previousFlows)
        if (updateReceivers) {
            logicSystem.updateAllReceivers(flow)
        }
        node.flow = flow
    }

    private fun FLOW.addImplication(statement: Implication) {
        logicSystem.addImplication(this, statement)
    }

    private fun FLOW.addTypeStatement(info: TypeStatement) {
        logicSystem.addTypeStatement(this, info)
    }

    private fun FLOW.removeAllAboutVariable(variable: RealVariable?) {
        if (variable == null) return
        logicSystem.removeAllAboutVariable(this, variable)
    }

    private val CFGNode<*>.previousFlow : FLOW
        get() = firstPreviousNode.flow
}

@DfaInternals
fun FirElement.extractReturnType(): ConeKotlinType = when (this) {
    is FirVariable<*> -> returnTypeRef.coneTypeUnsafe()
    is FirSimpleFunction -> receiverTypeRef?.coneTypeUnsafe()
    is FirAnonymousFunction -> receiverTypeRef?.coneTypeUnsafe()
    is FirRegularClass -> defaultType()
    is FirAnonymousObject -> typeRef.coneTypeUnsafe()
    else -> null
} ?: throw IllegalArgumentException("Unsupported fir: $this")
