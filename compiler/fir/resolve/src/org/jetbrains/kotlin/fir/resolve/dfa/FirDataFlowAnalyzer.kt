/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.dfa

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.PrivateForInline
import org.jetbrains.kotlin.fir.contracts.FirResolvedContractDescription
import org.jetbrains.kotlin.fir.contracts.description.ConeBooleanConstantReference
import org.jetbrains.kotlin.fir.contracts.description.ConeConditionalEffectDeclaration
import org.jetbrains.kotlin.fir.contracts.description.ConeConstantReference
import org.jetbrains.kotlin.fir.contracts.description.ConeReturnsEffectDeclaration
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.references.FirControlFlowGraphReference
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.resolve.PersistentImplicitReceiverStack
import org.jetbrains.kotlin.fir.resolve.ResolutionMode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.*
import org.jetbrains.kotlin.fir.resolve.dfa.contracts.buildContractFir
import org.jetbrains.kotlin.fir.resolve.dfa.contracts.createArgumentsMapping
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.FirAbstractBodyResolveTransformer
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.resultType
import org.jetbrains.kotlin.fir.symbols.AbstractFirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.CallableId
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.visitors.transformSingle
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.utils.addIfNotNull
import org.jetbrains.kotlin.utils.addToStdlib.runIf

class DataFlowAnalyzerContext<FLOW : Flow>(
    val graphBuilder: ControlFlowGraphBuilder,
    variableStorage: VariableStorage,
    flowOnNodes: MutableMap<CFGNode<*>, FLOW>,
    val variablesForWhenConditions: MutableMap<WhenBranchConditionExitNode, DataFlowVariable>
) {
    var flowOnNodes = flowOnNodes
        private set
    var variableStorage = variableStorage
        private set

    fun reset() {
        graphBuilder.reset()
        variablesForWhenConditions.clear()

        variableStorage = variableStorage.clear()
        flowOnNodes = mutableMapOf()
    }

    companion object {
        fun <FLOW : Flow> empty(session: FirSession) =
            DataFlowAnalyzerContext<FLOW>(
                ControlFlowGraphBuilder(), VariableStorage(session),
                mutableMapOf(), mutableMapOf()
            )
    }
}

@OptIn(DfaInternals::class)
abstract class FirDataFlowAnalyzer<FLOW : Flow>(
    protected val components: FirAbstractBodyResolveTransformer.BodyResolveTransformerComponents,
    private val context: DataFlowAnalyzerContext<FLOW>
) {
    companion object {
        internal val KOTLIN_BOOLEAN_NOT = CallableId(FqName("kotlin"), FqName("Boolean"), Name.identifier("not"))

        fun createFirDataFlowAnalyzer(
            components: FirAbstractBodyResolveTransformer.BodyResolveTransformerComponents,
            dataFlowAnalyzerContext: DataFlowAnalyzerContext<PersistentFlow>
        ): FirDataFlowAnalyzer<*> =
            object : FirDataFlowAnalyzer<PersistentFlow>(components, dataFlowAnalyzerContext) {
                private val receiverStack: PersistentImplicitReceiverStack
                    get() = components.implicitReceiverStack as PersistentImplicitReceiverStack

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

    private val graphBuilder get() = context.graphBuilder
    protected val variableStorage get() = context.variableStorage

    private var contractDescriptionVisitingMode = false

    protected val any = components.session.builtinTypes.anyType.type
    private val nullableNothing = components.session.builtinTypes.nullableNothingType.type

    @PrivateForInline
    var ignoreFunctionCalls: Boolean = false

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
            result.addIfNotNull(it.originalType)
            variable = it.variable
        }
        flow.getTypeStatement(variable)?.exactType?.let { result += it }
        return result.takeIf { it.isNotEmpty() }
    }

    fun returnExpressionsOfAnonymousFunction(function: FirAnonymousFunction): Collection<FirStatement> {
        return graphBuilder.returnExpressionsOfAnonymousFunction(function)
    }

    fun dropSubgraphFromCall(call: FirFunctionCall) {
        graphBuilder.dropSubgraphFromCall(call)
    }

    @OptIn(PrivateForInline::class)
    inline fun <T> withIgnoreFunctionCalls(block: () -> T): T {
        val oldValue = ignoreFunctionCalls
        ignoreFunctionCalls = true
        return try {
            block()
        } finally {
            ignoreFunctionCalls = oldValue
        }
    }

    // ----------------------------------- Named function -----------------------------------

    fun enterFunction(function: FirFunction<*>) {
        if (function is FirAnonymousFunction) {
            enterAnonymousFunction(function)
            return
        }
        val (functionEnterNode, localFunctionNode, previousNode) = graphBuilder.enterFunction(function)
        localFunctionNode?.mergeIncomingFlow()
        functionEnterNode.mergeIncomingFlow(shouldForkFlow = previousNode != null)
    }

    fun exitFunction(function: FirFunction<*>): FirControlFlowGraphReference {
        if (function is FirAnonymousFunction) {
            return exitAnonymousFunction(function)
        }
        val (node, graph) = graphBuilder.exitFunction(function)
        node.mergeIncomingFlow()
        if (!graphBuilder.isTopLevel()) {
            for (valueParameter in function.valueParameters) {
                variableStorage.removeRealVariable(valueParameter.symbol)
            }
        }
        val variableStorage = variableStorage
        val flowOnNodes = context.flowOnNodes

        if (graphBuilder.isTopLevel()) {
            context.reset()
        }
        return FirControlFlowGraphReferenceImpl(graph, DataFlowInfo(variableStorage, flowOnNodes))
    }

    // ----------------------------------- Anonymous function -----------------------------------

    private fun enterAnonymousFunction(anonymousFunction: FirAnonymousFunction) {
        val (postponedLambdaEnterNode, functionEnterNode) = graphBuilder.enterAnonymousFunction(anonymousFunction)
        // TODO: questionable
        postponedLambdaEnterNode?.mergeIncomingFlow()
        functionEnterNode.mergeIncomingFlow()
    }

    private fun exitAnonymousFunction(anonymousFunction: FirAnonymousFunction): FirControlFlowGraphReference {
        val (functionExitNode, postponedLambdaExitNode, graph) = graphBuilder.exitAnonymousFunction(anonymousFunction)
        // TODO: questionable
        postponedLambdaExitNode?.mergeIncomingFlow()
        functionExitNode.mergeIncomingFlow()
        return FirControlFlowGraphReferenceImpl(graph)
    }

    fun visitPostponedAnonymousFunction(anonymousFunction: FirAnonymousFunction) {
        val (enterNode, exitNode) = graphBuilder.visitPostponedAnonymousFunction(anonymousFunction)
        enterNode.mergeIncomingFlow()
        exitNode.mergeIncomingFlow()
        enterNode.flow = enterNode.flow.fork()
    }

    // ----------------------------------- Classes -----------------------------------

    fun enterClass() {
        graphBuilder.enterClass()
    }

    fun exitClass() {
        graphBuilder.exitClass()
    }

    fun exitRegularClass(klass: FirRegularClass): ControlFlowGraph {
        if (klass.isLocal && components.container !is FirClass<*>) return exitLocalClass(klass)
        return graphBuilder.exitClass(klass)
    }

    private fun exitLocalClass(klass: FirRegularClass): ControlFlowGraph {
        val (node, controlFlowGraph) = graphBuilder.exitLocalClass(klass)
        node.mergeIncomingFlow()
        return controlFlowGraph
    }

    fun exitAnonymousObject(anonymousObject: FirAnonymousObject): ControlFlowGraph {
        val (node, controlFlowGraph) = graphBuilder.exitAnonymousObject(anonymousObject)
        node.mergeIncomingFlow()
        return controlFlowGraph
    }

    // ----------------------------------- Value parameters (and it's defaults) -----------------------------------

    fun enterValueParameter(valueParameter: FirValueParameter) {
        graphBuilder.enterValueParameter(valueParameter)?.mergeIncomingFlow(shouldForkFlow = true)
    }

    fun exitValueParameter(valueParameter: FirValueParameter): ControlFlowGraph? {
        val (node, graph) = graphBuilder.exitValueParameter(valueParameter) ?: return null
        node.mergeIncomingFlow()
        return graph
    }

    // ----------------------------------- Property -----------------------------------

    fun enterProperty(property: FirProperty) {
        graphBuilder.enterProperty(property)?.mergeIncomingFlow()
    }

    fun exitProperty(property: FirProperty): ControlFlowGraph? {
        val (node, graph) = graphBuilder.exitProperty(property) ?: return null
        node.mergeIncomingFlow()
        return graph
    }

    // ----------------------------------- Delegate -----------------------------------

    fun enterDelegateExpression() {
        graphBuilder.enterDelegateExpression()
    }

    fun exitDelegateExpression() {
        graphBuilder.exitDelegateExpression()
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
        val type = typeOperatorCall.conversionTypeRef.coneType
        val operandVariable = variableStorage.getOrCreateVariable(node.previousFlow, typeOperatorCall.argument)
        val flow = node.flow

        when (val operation = typeOperatorCall.operation) {
            FirOperation.IS, FirOperation.NOT_IS -> {
                val expressionVariable = variableStorage.createSyntheticVariable(typeOperatorCall)
                val isNotNullCheck = type.nullability == ConeNullability.NOT_NULL
                val isRegularIs = operation == FirOperation.IS
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
                        flow.addImplication((expressionVariable eq isRegularIs) implies (operandVariable typeEq any))
                        flow.addImplication((expressionVariable eq isRegularIs) implies (operandVariable notEq null))
                    }

                } else {
                    if (isNotNullCheck) {
                        flow.addImplication((expressionVariable eq isRegularIs) implies (operandVariable notEq null))
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

    fun exitComparisonExpressionCall(comparisonExpression: FirComparisonExpression) {
        graphBuilder.exitComparisonExpression(comparisonExpression).mergeIncomingFlow()
    }

    fun exitEqualityOperatorCall(equalityOperatorCall: FirEqualityOperatorCall) {
        val node = graphBuilder.exitEqualityOperatorCall(equalityOperatorCall).mergeIncomingFlow()
        val operation = equalityOperatorCall.operation
        val leftOperand = equalityOperatorCall.arguments[0]
        val rightOperand = equalityOperatorCall.arguments[1]

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

    // const != null
    private fun processEqWithConst(
        node: EqualityOperatorCallNode, operand: FirExpression, const: FirConstExpression<*>, operation: FirOperation
    ) {
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

    private fun processEq(
        node: EqualityOperatorCallNode, leftOperand: FirExpression, rightOperand: FirExpression, operation: FirOperation
    ) {
        val leftIsNullable = leftOperand.coneType.isMarkedNullable
        val rightIsNullable = rightOperand.coneType.isMarkedNullable
        // left == right && right not null -> left != null
        when {
            leftIsNullable && rightIsNullable -> return
            leftIsNullable -> processEqNull(node, leftOperand, operation.invert())
            rightIsNullable -> processEqNull(node, rightOperand, operation.invert())
        }

        if (operation == FirOperation.IDENTITY || operation == FirOperation.NOT_IDENTITY) {
            processIdentity(node, leftOperand, rightOperand, operation)
        }
    }

    private fun processEqNull(node: EqualityOperatorCallNode, operand: FirExpression, operation: FirOperation) {
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

    private fun processIdentity(
        node: EqualityOperatorCallNode, leftOperand: FirExpression, rightOperand: FirExpression, operation: FirOperation
    ) {
        val flow = node.flow
        val expressionVariable = variableStorage.getOrCreateVariable(node.previousFlow, node.fir)
        val leftOperandVariable = variableStorage.getOrCreateVariable(node.previousFlow, leftOperand)
        val rightOperandVariable = variableStorage.getOrCreateVariable(node.previousFlow, rightOperand)
        val leftOperandType = leftOperand.coneType
        val rightOperandType = rightOperand.coneType
        val isEq = operation.isEq()

        if (leftOperandVariable.isReal()) {
            flow.addImplication((expressionVariable eq isEq) implies (leftOperandVariable typeEq rightOperandType))
            flow.addImplication((expressionVariable notEq isEq) implies (leftOperandVariable typeNotEq rightOperandType))
        }

        if (rightOperandVariable.isReal()) {
            flow.addImplication((expressionVariable eq isEq) implies (rightOperandVariable typeEq leftOperandType))
            flow.addImplication((expressionVariable notEq isEq) implies (rightOperandVariable typeNotEq leftOperandType))
        }

        node.flow = flow
    }

    // ----------------------------------- Jump -----------------------------------

    fun exitJump(jump: FirJump<*>) {
        graphBuilder.exitJump(jump).mergeIncomingFlow()
    }

    // ----------------------------------- Check not null call -----------------------------------

    fun exitCheckNotNullCall(checkNotNullCall: FirCheckNotNullCall, callCompleted: Boolean) {
        // Add `Any` to the set of possible types; the intersection type `T? & Any` will be reduced to `T` after smartcast.
        val (node, unionNode) = graphBuilder.exitCheckNotNullCall(checkNotNullCall, callCompleted)
        node.mergeIncomingFlow()
        val argument = checkNotNullCall.argument
        variableStorage.getOrCreateRealVariable(node.previousFlow, argument.symbol, argument)?.let { operandVariable ->
            node.flow.addTypeStatement(operandVariable typeEq any)
            logicSystem.approveStatementsInsideFlow(
                node.flow,
                operandVariable notEq null,
                shouldRemoveSynthetics = true,
                shouldForkFlow = false
            )
        }
        unionNode?.let { unionFlowFromArguments(it) }
    }

    // ----------------------------------- When -----------------------------------

    fun enterWhenExpression(whenExpression: FirWhenExpression) {
        graphBuilder.enterWhenExpression(whenExpression).mergeIncomingFlow()
    }

    fun enterWhenBranchCondition(whenBranch: FirWhenBranch) {
        val node = graphBuilder.enterWhenBranchCondition(whenBranch).mergeIncomingFlow(updateReceivers = true)
        val previousNode = node.previousNodes.single()
        if (previousNode is WhenBranchConditionExitNode) {
            val conditionVariable = context.variablesForWhenConditions.remove(previousNode)!!
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
        context.variablesForWhenConditions[conditionExitNode] = conditionVariable
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
                val conditionVariable = context.variablesForWhenConditions.remove(previousConditionExitNode)!!
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

    fun enterFinallyBlock() {
        // TODO
        graphBuilder.enterFinallyBlock().mergeIncomingFlow()
    }

    fun exitFinallyBlock(tryExpression: FirTryExpression) {
        // TODO
        graphBuilder.exitFinallyBlock(tryExpression).mergeIncomingFlow()
    }

    fun exitTryExpression(callCompleted: Boolean) {
        // TODO
        val (tryExpressionExitNode, unionNode) = graphBuilder.exitTryExpression(callCompleted)
        tryExpressionExitNode.mergeIncomingFlow()
        unionNode?.let { unionFlowFromArguments(it) }
    }

    // ----------------------------------- Resolvable call -----------------------------------

    // Intentionally left empty for potential future needs (call sites are preserved)
    fun enterQualifiedAccessExpression() {}

    fun exitQualifiedAccessExpression(qualifiedAccessExpression: FirQualifiedAccessExpression) {
        graphBuilder.exitQualifiedAccessExpression(qualifiedAccessExpression).mergeIncomingFlow()
        processConditionalContract(qualifiedAccessExpression)
    }

    fun enterSafeCallAfterNullCheck(safeCall: FirSafeCallExpression) {
        val node = graphBuilder.enterSafeCall(safeCall).mergeIncomingFlow()
        val previousNode = node.firstPreviousNode
        val shouldFork: Boolean
        var flow = if (previousNode is ExitSafeCallNode) {
            shouldFork = false
            previousNode.secondPreviousNode?.flow ?: node.flow
        } else {
            shouldFork = true
            node.flow
        }

        safeCall.receiver.let { receiver ->
            val type = receiver.coneType.takeIf { it.isMarkedNullable }
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
                shouldRemoveSynthetics = false
            )
        }

        node.flow = flow
    }

    fun exitSafeCall(safeCall: FirSafeCallExpression) {
        val node = graphBuilder.exitSafeCall().mergeIncomingFlow()
        val previousFlow = node.previousFlow

        val variable = variableStorage.getOrCreateVariable(previousFlow, safeCall)
        val receiverVariable = when (variable) {
            // There is some bug with invokes. See KT-36014
            is RealVariable -> variable.explicitReceiverVariable ?: return
            is SyntheticVariable -> variableStorage.getOrCreateVariable(previousFlow, safeCall.receiver)
        }
        logicSystem.addImplication(node.flow, (variable notEq null) implies (receiverVariable notEq null))
        if (receiverVariable.isReal()) {
            logicSystem.addImplication(node.flow, (variable notEq null) implies (receiverVariable typeEq any))
        }
    }

    fun exitResolvedQualifierNode(resolvedQualifier: FirResolvedQualifier) {
        graphBuilder.exitResolvedQualifierNode(resolvedQualifier).mergeIncomingFlow()
    }

    fun enterCall() {
        graphBuilder.enterCall()
    }

    @OptIn(PrivateForInline::class)
    fun exitFunctionCall(functionCall: FirFunctionCall, callCompleted: Boolean) {
        if (ignoreFunctionCalls) {
            graphBuilder.exitIgnoredCall(functionCall)
            return
        }
        val (functionCallNode, unionNode) = graphBuilder.exitFunctionCall(functionCall, callCompleted)
        unionNode?.let { unionFlowFromArguments(it) }
        functionCallNode.mergeIncomingFlow()
        if (functionCall.isBooleanNot()) {
            exitBooleanNot(functionCall, functionCallNode)
        }
        processConditionalContract(functionCall)
    }

    fun exitDelegatedConstructorCall(call: FirDelegatedConstructorCall, callCompleted: Boolean) {
        val (callNode, unionNode) = graphBuilder.exitDelegatedConstructorCall(call, callCompleted)
        unionNode?.let { unionFlowFromArguments(it) }
        callNode.mergeIncomingFlow()
    }


    private fun unionFlowFromArguments(node: UnionFunctionCallArgumentsNode) {
        node.flow = logicSystem.unionFlow(node.previousNodes.map { it.flow }).also {
            logicSystem.updateAllReceivers(it)
        }
    }

    private fun processConditionalContract(qualifiedAccess: FirQualifiedAccess) {
        val owner: FirContractDescriptionOwner? = when (qualifiedAccess) {
            is FirFunctionCall -> qualifiedAccess.toResolvedCallableSymbol()?.fir as? FirSimpleFunction
            is FirQualifiedAccessExpression -> {
                val property = (qualifiedAccess.calleeReference as? FirResolvedNamedReference)?.resolvedSymbol?.fir as? FirProperty
                property?.getter
            }
            is FirVariableAssignment -> {
                val property = (qualifiedAccess.lValue as? FirResolvedNamedReference)?.resolvedSymbol?.fir as? FirProperty
                property?.setter
            }
            else -> null
        }

        val contractDescription = owner?.contractDescription as? FirResolvedContractDescription ?: return
        val conditionalEffects = contractDescription.effects.map { it.effect }.filterIsInstance<ConeConditionalEffectDeclaration>()
        if (conditionalEffects.isEmpty()) return
        val argumentsMapping = createArgumentsMapping(qualifiedAccess) ?: return
        contractDescriptionVisitingMode = true
        graphBuilder.enterContract(qualifiedAccess).mergeIncomingFlow()
        val lastFlow = graphBuilder.lastNode.flow
        val functionCallVariable = variableStorage.getOrCreateVariable(lastFlow, qualifiedAccess)
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
                        filter = { it.condition.operation == Operation.EqTrue },
                        transform = {
                            when (value) {
                                ConeBooleanConstantReference.TRUE -> it
                                ConeBooleanConstantReference.FALSE -> it.invertCondition()
                                else -> throw IllegalStateException()
                            }
                        }
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
        graphBuilder.exitContract(qualifiedAccess).mergeIncomingFlow(updateReceivers = true)
        contractDescriptionVisitingMode = false
    }

    fun exitConstExpression(constExpression: FirConstExpression<*>) {
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
        if (property.isLocal || !property.isVar) {
            exitVariableInitialization(node, assignment.rValue, property, assignment)
        }
        processConditionalContract(assignment)
    }

    private fun exitVariableInitialization(
        node: CFGNode<*>,
        initializer: FirExpression,
        property: FirProperty,
        assignment: FirVariableAssignment?
    ) {
        val flow = node.flow
        val propertyVariable = variableStorage.getOrCreateRealVariableWithoutUnwrappingAlias(flow, property.symbol, assignment ?: property)
        val isAssignment = assignment != null
        if (isAssignment) {
            logicSystem.removeLocalVariableAlias(flow, propertyVariable)
            flow.removeAllAboutVariable(propertyVariable)
        }

        variableStorage.getOrCreateRealVariable(flow, initializer.symbol, initializer)?.let { initializerVariable ->
            logicSystem.addLocalVariableAlias(
                flow, propertyVariable,
                RealVariableAndType(initializerVariable, initializer.coneType)
            )
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
            flow.addTypeStatement(propertyVariable typeEq initializer.typeRef.coneType)
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
        @Suppress("UnnecessaryVariable")
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

        if (!node.leftOperandNode.isDead && node.rightOperandNode.isDead) {
            /*
             * If there was a jump from right argument then we know that we well exit from
             *   boolean operator only if right operand was not executed
             *
             *   a && return => a == false
             *   a || return => a == true
             */
            logicSystem.approveStatementsInsideFlow(
                flow,
                leftVariable eq !isAnd,
                shouldForkFlow = false,
                shouldRemoveSynthetics = true
            )
        } else {
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
            val rightIsFalse =
                logicSystem.approveOperationStatement(flowFromRight, rightVariable eq onlyLeftEvaluated, conditionalFromRight)
            approvedIfFalse.mergeTypeStatements(logicSystem.orForTypeStatements(leftIsFalse, rightIsFalse))
            approvedIfFalse.values.forEach { info ->
                flow.addImplication((operatorVariable eq onlyLeftEvaluated) implies info)
            }
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
        graphBuilder.enterInitBlock(initBlock).let { (node, prevNode) ->
            if (prevNode != null) {
                node.flow = logicSystem.forkFlow(prevNode.flow)
            } else {
                node.mergeIncomingFlow()
            }
        }
    }

    fun exitInitBlock(initBlock: FirAnonymousInitializer): ControlFlowGraph {
        val (node, controlFlowGraph) = graphBuilder.exitInitBlock(initBlock)
        node.mergeIncomingFlow()
        return controlFlowGraph
    }

    // ----------------------------------- Contract description -----------------------------------

    fun enterContractDescription() {
        graphBuilder.enterContractDescription().mergeIncomingFlow()
    }

    fun exitContractDescription() {
        graphBuilder.exitContractDescription()
    }

    // ----------------------------------- Elvis -----------------------------------

    fun exitElvisLhs(elvisExpression: FirElvisExpression) {
        val (lhsExitNode, lhsIsNotNullNode, rhsEnterNode) = graphBuilder.exitElvisLhs(elvisExpression)
        lhsExitNode.mergeIncomingFlow()
        val flow = lhsExitNode.flow
        val lhsVariable = variableStorage.getOrCreateVariable(flow, elvisExpression.lhs)
        rhsEnterNode.flow = logicSystem.approveStatementsInsideFlow(
            flow,
            lhsVariable eq null,
            shouldForkFlow = true,
            shouldRemoveSynthetics = false
        )
        lhsIsNotNullNode.flow = logicSystem.approveStatementsInsideFlow(
            flow,
            lhsVariable notEq null,
            shouldForkFlow = true,
            shouldRemoveSynthetics = false
        ).also {
            if (lhsVariable.isReal()) {
                it.addTypeStatement(lhsVariable typeEq any)
            }
        }
    }

    fun exitElvis() {
        graphBuilder.exitElvis().mergeIncomingFlow()
    }

    // ------------------------------------------------------ Utils ------------------------------------------------------

    private var CFGNode<*>.flow: FLOW
        get() = context.flowOnNodes.getValue(this.origin)
        set(value) {
            context.flowOnNodes[this.origin] = value
        }

    private val CFGNode<*>.origin: CFGNode<*> get() = if (this is StubNode) firstPreviousNode else this

    private fun <T : CFGNode<*>> T.mergeIncomingFlow(
        updateReceivers: Boolean = false,
        shouldForkFlow: Boolean = false
    ): T = this.also { node ->
        val previousFlows = if (node.isDead)
            node.previousNodes.mapNotNull { runIf(!node.incomingEdges.getValue(it).isBack) { it.flow } }
        else
            node.previousNodes.mapNotNull { prev -> prev.takeIf { node.incomingEdges.getValue(it).usedInDfa }?.flow }
        var flow = logicSystem.joinFlow(previousFlows)
        if (updateReceivers) {
            logicSystem.updateAllReceivers(flow)
        }
        if (shouldForkFlow) {
            flow = flow.fork()
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

    private fun FLOW.fork(): FLOW {
        return logicSystem.forkFlow(this)
    }

    private val CFGNode<*>.previousFlow: FLOW
        get() = firstPreviousNode.flow
}
