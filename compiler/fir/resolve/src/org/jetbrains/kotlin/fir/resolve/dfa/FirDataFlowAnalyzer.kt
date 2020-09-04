/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.dfa

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.PrivateForInline
import org.jetbrains.kotlin.fir.contracts.FirResolvedContractDescription
import org.jetbrains.kotlin.fir.contracts.coneEffects
import org.jetbrains.kotlin.fir.contracts.description.*
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.impl.FirNoReceiverExpression
import org.jetbrains.kotlin.fir.references.FirControlFlowGraphReference
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.resolve.PersistentImplicitReceiverStack
import org.jetbrains.kotlin.fir.resolve.ResolutionMode
import org.jetbrains.kotlin.fir.resolve.calls.candidate
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.*
import org.jetbrains.kotlin.fir.resolve.dfa.contracts.ConeBooleanExpressionToFirVisitor
import org.jetbrains.kotlin.fir.resolve.dfa.contracts.buildContractFir
import org.jetbrains.kotlin.fir.resolve.dfa.contracts.createArgumentsMapping
import org.jetbrains.kotlin.fir.resolve.dfa.contracts.unwrap
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.FirAbstractBodyResolveTransformer
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.resultType
import org.jetbrains.kotlin.fir.symbols.AbstractFirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.CallableId
import org.jetbrains.kotlin.fir.symbols.impl.FirAnonymousFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
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
    val variablesForWhenConditions: MutableMap<WhenBranchConditionExitNode, DataFlowVariable>,
    val lambdaFunctionCalls: MutableMap<FirAnonymousFunctionSymbol, FirFunctionCall>
) {
    var flowOnNodes = flowOnNodes
        private set
    var variableStorage = variableStorage
        private set

    fun reset() {
        graphBuilder.reset()
        variablesForWhenConditions.clear()
        lambdaFunctionCalls.clear()

        variableStorage = variableStorage.clear()
        flowOnNodes = mutableMapOf()
    }

    companion object {
        fun <FLOW : Flow> empty(session: FirSession) =
            DataFlowAnalyzerContext<FLOW>(
                ControlFlowGraphBuilder(), VariableStorage(session),
                mutableMapOf(), mutableMapOf(), mutableMapOf()
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

    protected val dataFlowResolver = object : DataFlowResolver<FLOW>() {
        override var CFGNode<*>.flow: FLOW
            get() = context.flowOnNodes.getValue(this.origin)
            set(value) {
                context.flowOnNodes[this.origin] = value
            }

        override val logicSystem: LogicSystem<FLOW> get() = this@FirDataFlowAnalyzer.logicSystem
        override val variableStorage: VariableStorage get() = context.variableStorage
        override val session: FirSession get() = components.session
        override val variablesForWhenConditions: MutableMap<WhenBranchConditionExitNode, DataFlowVariable> get() = context.variablesForWhenConditions
    }

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

    fun enterFunctionCallWithCandidate(functionCall: FirFunctionCall) {
        for (argument in functionCall.arguments) {
            val anonymousFunction = argument.unwrap() as? FirAnonymousFunction ?: continue
            if (anonymousFunction.isLambda) context.lambdaFunctionCalls[anonymousFunction.symbol] = functionCall
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

        if (anonymousFunction.isLambda) {
            val functionCall = context.lambdaFunctionCalls.remove(anonymousFunction.symbol) ?: return
            processTrueInContract(anonymousFunction, functionCall, functionEnterNode)
        }
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
        dataFlowResolver.processPostponedAnonymousFunction(enterNode)
    }

    private fun processTrueInContract(anonymousFunction: FirAnonymousFunction, functionCall: FirFunctionCall, node: FunctionEnterNode) {
        if (anonymousFunction.invocationKind == null) return
        val candidate = functionCall.candidate() ?: return
        val function = (candidate.symbol as? FirCallableSymbol<*>)?.fir as? FirSimpleFunction ?: return
        val expressionToParameter = candidate.argumentMapping ?: return
        val effects = function.contractDescription.coneEffects ?: return
        if (effects.none { it is ConeTrueInEffectDeclaration }) return

        val valueParameter = expressionToParameter.entries.find { it.key.unwrap() == anonymousFunction }?.value ?: return
        val valueParameterIndex = function.valueParameters.indexOf(valueParameter)

        val indexToExpression = mutableMapOf<Int, FirExpression>()
        val parameterToExpression = expressionToParameter.map { it.value to it.key.unwrap() }.toMap()
        for ((index, parameter) in function.valueParameters.withIndex()) {
            val expression = parameterToExpression[parameter] ?: parameter.defaultValue
            if (expression != null) indexToExpression[index] = expression
        }
        candidate.extensionReceiverExpression().takeIf { it != FirNoReceiverExpression }?.let { indexToExpression[-1] = it }
            ?: candidate.dispatchReceiverExpression().takeIf { it != FirNoReceiverExpression }?.let { indexToExpression[-1] = it }

        val lastFlow = node.flow

        for (effect in effects) {
            if (effect is ConeTrueInEffectDeclaration && effect.target.parameterIndex == valueParameterIndex) {
                val condition = effect.condition.accept(ConeBooleanExpressionToFirVisitor, indexToExpression) ?: continue
                condition.transformSingle(components.transformer, ResolutionMode.ContextDependent)
                val conditionVariable = variableStorage.getOrCreateVariable(lastFlow, condition)

                node.flow = logicSystem.approveStatementsInsideFlow(
                    node.flow,
                    conditionVariable eq true,
                    shouldForkFlow = false,
                    shouldRemoveSynthetics = true
                )
            }
        }
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
        dataFlowResolver.processTypeOperatorCall(node)
    }

    fun exitComparisonExpressionCall(comparisonExpression: FirComparisonExpression) {
        graphBuilder.exitComparisonExpression(comparisonExpression).mergeIncomingFlow()
    }

    fun exitEqualityOperatorCall(equalityOperatorCall: FirEqualityOperatorCall) {
        val node = graphBuilder.exitEqualityOperatorCall(equalityOperatorCall).mergeIncomingFlow()
        dataFlowResolver.processEqualityOperatorCall(node)
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
        dataFlowResolver.processCheckNotNullCall(node)
        dataFlowResolver.processUnionFunctionCallArguments(unionNode)
    }

    // ----------------------------------- When -----------------------------------

    fun enterWhenExpression(whenExpression: FirWhenExpression) {
        graphBuilder.enterWhenExpression(whenExpression).mergeIncomingFlow()
    }

    fun enterWhenBranchCondition(whenBranch: FirWhenBranch) {
        val node = graphBuilder.enterWhenBranchCondition(whenBranch).mergeIncomingFlow(updateReceivers = true)
        dataFlowResolver.processWhenBranchConditionEnter(node)
    }

    fun exitWhenBranchCondition(whenBranch: FirWhenBranch) {
        val (conditionExitNode, branchEnterNode) = graphBuilder.exitWhenBranchCondition(whenBranch)
        conditionExitNode.mergeIncomingFlow()
        dataFlowResolver.processWhenBranchConditionExit(conditionExitNode, branchEnterNode)
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
                dataFlowResolver.processSyntheticElse(syntheticElseNode, previousConditionExitNode)
            } else {
                syntheticElseNode.mergeIncomingFlow()
            }
        }
        whenExitNode.mergeIncomingFlow(updateReceivers = true)
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
        dataFlowResolver.processWhileLoopCondition(loopConditionExitNode, loopBlockEnterNode)
    }

    fun exitWhileLoop(loop: FirLoop) {
        val (blockExitNode, exitNode) = graphBuilder.exitWhileLoop(loop)
        blockExitNode.mergeIncomingFlow()
        exitNode.mergeIncomingFlow()
        dataFlowResolver.processLoop(exitNode)
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
        dataFlowResolver.processLoop(loopExitNode)
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
        dataFlowResolver.processUnionFunctionCallArguments(unionNode)
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
        dataFlowResolver.processSafeCallEnter(node)
    }

    fun exitSafeCall(safeCall: FirSafeCallExpression) {
        val node = graphBuilder.exitSafeCall().mergeIncomingFlow()
        dataFlowResolver.processSafeCallExit(node)
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
        dataFlowResolver.processUnionFunctionCallArguments(unionNode)
        functionCallNode.mergeIncomingFlow()
        if (functionCall.isBooleanNot()) {
            dataFlowResolver.processBooleanNot(functionCallNode)
        }
        processConditionalContract(functionCall)
    }

    fun exitDelegatedConstructorCall(call: FirDelegatedConstructorCall, callCompleted: Boolean) {
        val (callNode, unionNode) = graphBuilder.exitDelegatedConstructorCall(call, callCompleted)
        dataFlowResolver.processUnionFunctionCallArguments(unionNode)
        callNode.mergeIncomingFlow()
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
        dataFlowResolver.processVariableInitialization(node, initializer, variable, assignment = null)
    }

    fun exitVariableAssignment(assignment: FirVariableAssignment) {
        val node = graphBuilder.exitVariableAssignment(assignment).mergeIncomingFlow()
        val property = (assignment.lValue as? FirResolvedNamedReference)?.resolvedSymbol?.fir as? FirProperty ?: return
        // TODO: add unstable smartcast
        if (property.isLocal || !property.isVar) {
            dataFlowResolver.processVariableInitialization(node, assignment.rValue, property, assignment)
        }
        processConditionalContract(assignment)
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
        dataFlowResolver.processLeftArgumentOfBinaryBooleanOperator(leftNode, rightNode, isAnd = true)
    }

    fun exitBinaryAnd(binaryLogicExpression: FirBinaryLogicExpression) {
        val node = graphBuilder.exitBinaryAnd(binaryLogicExpression).mergeIncomingFlow()
        dataFlowResolver.processBinaryBooleanOperator(binaryLogicExpression, node, isAnd = true)
    }

    fun enterBinaryOr(binaryLogicExpression: FirBinaryLogicExpression) {
        graphBuilder.enterBinaryOr(binaryLogicExpression).mergeIncomingFlow()
    }

    fun exitLeftBinaryOrArgument(binaryLogicExpression: FirBinaryLogicExpression) {
        val (leftNode, rightNode) = graphBuilder.exitLeftBinaryOrArgument(binaryLogicExpression)
        dataFlowResolver.processLeftArgumentOfBinaryBooleanOperator(leftNode, rightNode, isAnd = false)
    }

    fun exitBinaryOr(binaryLogicExpression: FirBinaryLogicExpression) {
        val node = graphBuilder.exitBinaryOr(binaryLogicExpression).mergeIncomingFlow()
        dataFlowResolver.processBinaryBooleanOperator(binaryLogicExpression, node, isAnd = false)
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
        val (node, prevNode) = graphBuilder.enterInitBlock(initBlock)
        if (prevNode != null) {
            dataFlowResolver.processInitBlock(node, prevNode)
        } else {
            node.mergeIncomingFlow()
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
        dataFlowResolver.processElvis(lhsExitNode, lhsIsNotNullNode, rhsEnterNode)
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

    private fun FLOW.fork(): FLOW {
        return logicSystem.forkFlow(this)
    }
}