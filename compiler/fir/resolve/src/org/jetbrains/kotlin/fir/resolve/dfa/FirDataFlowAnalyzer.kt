/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.dfa

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.contracts.description.EventOccurrencesRange
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.contracts.FirResolvedContractDescription
import org.jetbrains.kotlin.fir.contracts.description.ConeBooleanConstantReference
import org.jetbrains.kotlin.fir.contracts.description.ConeConditionalEffectDeclaration
import org.jetbrains.kotlin.fir.contracts.description.ConeConstantReference
import org.jetbrains.kotlin.fir.contracts.description.ConeReturnsEffectDeclaration
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.impl.FirDefaultPropertyAccessor
import org.jetbrains.kotlin.fir.declarations.utils.isLocal
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.references.FirControlFlowGraphReference
import org.jetbrains.kotlin.fir.resolve.*
import org.jetbrains.kotlin.fir.resolve.calls.ImplicitReceiverValue
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.*
import org.jetbrains.kotlin.fir.resolve.dfa.contracts.buildContractFir
import org.jetbrains.kotlin.fir.resolve.dfa.contracts.createArgumentsMapping
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutor
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutorByMap
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.FirAbstractBodyResolveTransformer
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.resultType
import org.jetbrains.kotlin.fir.scopes.getFunctions
import org.jetbrains.kotlin.fir.scopes.impl.declaredMemberScope
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeParameterSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirVariableSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.visitors.transformSingle
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.types.ConstantValueKind
import org.jetbrains.kotlin.util.OperatorNameConventions
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull

class DataFlowAnalyzerContext<FLOW : Flow>(
    val graphBuilder: ControlFlowGraphBuilder,
    variableStorage: VariableStorageImpl,
    flowOnNodes: MutableMap<CFGNode<*>, FLOW>,
    val variablesForWhenConditions: MutableMap<WhenBranchConditionExitNode, DataFlowVariable>,
    val preliminaryLoopVisitor: PreliminaryLoopVisitor
) {
    var flowOnNodes = flowOnNodes
        private set
    var variableStorage = variableStorage
        private set

    internal var firLocalVariableAssignmentAnalyzer: FirLocalVariableAssignmentAnalyzer? = null

    private var assignmentCounter = 0

    fun newAssignmentIndex(): Int {
        return assignmentCounter++
    }

    fun reset() {
        graphBuilder.reset()
        variablesForWhenConditions.clear()

        variableStorage = variableStorage.clear()
        flowOnNodes = mutableMapOf()

        preliminaryLoopVisitor.resetState()
        firLocalVariableAssignmentAnalyzer = null
    }

    companion object {
        fun <FLOW : Flow> empty(session: FirSession): DataFlowAnalyzerContext<FLOW> =
            DataFlowAnalyzerContext(
                ControlFlowGraphBuilder(), VariableStorageImpl(session),
                mutableMapOf(), mutableMapOf(), PreliminaryLoopVisitor()
            )
    }
}

@OptIn(DfaInternals::class)
abstract class FirDataFlowAnalyzer<FLOW : Flow>(
    protected val components: FirAbstractBodyResolveTransformer.BodyResolveTransformerComponents,
    private val context: DataFlowAnalyzerContext<FLOW>
) {
    companion object {
        fun createFirDataFlowAnalyzer(
            components: FirAbstractBodyResolveTransformer.BodyResolveTransformerComponents,
            dataFlowAnalyzerContext: DataFlowAnalyzerContext<PersistentFlow>
        ): FirDataFlowAnalyzer<*> =
            object : FirDataFlowAnalyzer<PersistentFlow>(components, dataFlowAnalyzerContext) {
                override val receiverStack: PersistentImplicitReceiverStack
                    get() = components.implicitReceiverStack as PersistentImplicitReceiverStack

                private val visibilityChecker = components.session.visibilityChecker
                private val typeContext = components.session.typeContext

                override fun receiverUpdated(symbol: FirBasedSymbol<*>, types: Set<ConeKotlinType>?) {
                    val index = receiverStack.getReceiverIndex(symbol) ?: return
                    val originalType = receiverStack.getOriginalType(index)
                    val type = if (types?.isNotEmpty() == true) {
                        typeContext.intersectTypes(types.toMutableList().also { it += originalType })
                    } else {
                        originalType
                    }
                    receiverStack.replaceReceiverType(index, type)
                }

                override val logicSystem: PersistentLogicSystem =
                    object : PersistentLogicSystem(components.session.typeContext) {
                        override val variableStorage: VariableStorageImpl
                            get() = dataFlowAnalyzerContext.variableStorage

                        override fun ConeKotlinType.isAcceptableForSmartcast(): Boolean {
                            if (this.isNullableNothing) return false
                            return when (this) {
                                is ConeClassLikeType -> {
                                    val symbol =
                                        fullyExpandedType(components.session).lookupTag.toSymbol(components.session) ?: return false
                                    val declaration = symbol.fir as? FirRegularClass ?: return true
                                    visibilityChecker.isClassLikeVisible(
                                        declaration,
                                        components.session,
                                        components.context.file,
                                        components.context.containers,
                                    )
                                }
                                is ConeTypeParameterType -> true
                                is ConeFlexibleType -> lowerBound.isAcceptableForSmartcast() && upperBound.isAcceptableForSmartcast()
                                is ConeIntersectionType -> intersectedTypes.all { it.isAcceptableForSmartcast() }
                                is ConeDefinitelyNotNullType -> original.isAcceptableForSmartcast()
                                else -> false
                            }
                        }
                    }
            }
    }

    protected abstract val logicSystem: LogicSystem<FLOW>
    protected abstract val receiverStack: Iterable<ImplicitReceiverValue<*>>
    protected abstract fun receiverUpdated(symbol: FirBasedSymbol<*>, types: Set<ConeKotlinType>?)

    private val graphBuilder get() = context.graphBuilder
    private val variableStorage get() = context.variableStorage

    private var contractDescriptionVisitingMode = false

    private val any = components.session.builtinTypes.anyType.type
    private val nullableNothing = components.session.builtinTypes.nullableNothingType.type

    @PrivateForInline
    var ignoreFunctionCalls: Boolean = false

    // ----------------------------------- Requests -----------------------------------

    fun isAccessToUnstableLocalVariable(expression: FirExpression): Boolean {
        val qualifiedAccessExpression = when (expression) {
            is FirSmartCastExpression -> expression.originalExpression as FirQualifiedAccessExpression
            is FirQualifiedAccessExpression -> expression
            is FirWhenSubjectExpression -> {
                val whenExpression = expression.whenRef.value
                when {
                    whenExpression.subjectVariable != null -> return true
                    else -> whenExpression.subject as? FirQualifiedAccessExpression
                }
            }
            else -> null
        } ?: return false
        return context.firLocalVariableAssignmentAnalyzer?.isAccessToUnstableLocalVariable(qualifiedAccessExpression) == true
    }

    fun getTypeUsingSmartcastInfo(whenSubjectExpression: FirWhenSubjectExpression): Pair<PropertyStability, MutableList<ConeKotlinType>>? {
        val symbol = whenSubjectExpression.symbol ?: return null
        return getTypeUsingSmartcastInfo(symbol, whenSubjectExpression)
    }

    fun getTypeUsingSmartcastInfo(qualifiedAccessExpression: FirQualifiedAccessExpression): Pair<PropertyStability, MutableList<ConeKotlinType>>? {
        /*
         * DataFlowAnalyzer holds variables only for declarations that have some smartcast (or can have)
         * If there is no useful information there is no data flow variable also
         */
        val symbol: FirBasedSymbol<*> = qualifiedAccessExpression.symbol ?: return null
        return getTypeUsingSmartcastInfo(symbol, qualifiedAccessExpression)
    }

    protected open fun getTypeUsingSmartcastInfo(
        symbol: FirBasedSymbol<*>,
        expression: FirExpression
    ): Pair<PropertyStability, MutableList<ConeKotlinType>>? {
        val flow = graphBuilder.lastNode.flow
        val variable = variableStorage.getRealVariableWithoutUnwrappingAlias(flow, symbol, expression) ?: return null
        return flow.getType(variable)?.takeIf { it.isNotEmpty() }?.let { variable.stability to it.toMutableList() }
    }

    fun returnExpressionsOfAnonymousFunction(function: FirAnonymousFunction): Collection<FirStatement> {
        return graphBuilder.returnExpressionsOfAnonymousFunction(function)
    }

    fun isThereControlFlowInfoForAnonymousFunction(function: FirAnonymousFunction): Boolean =
        graphBuilder.isThereControlFlowInfoForAnonymousFunction(function)

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

    fun enterFunction(function: FirFunction) {
        if (function is FirDefaultPropertyAccessor) return
        if (function is FirAnonymousFunction) {
            enterAnonymousFunction(function)
            return
        }
        // All non-lambda function are treated as concurrent since we do not make any assumption about when and how it's invoked.
        getOrCreateLocalVariableAssignmentAnalyzer(function)?.enterLocalFunction(function)

        val (functionEnterNode, localFunctionNode) = graphBuilder.enterFunction(function)
        localFunctionNode?.mergeIncomingFlow()
        functionEnterNode.mergeIncomingFlow()
    }

    fun exitFunction(function: FirFunction): FirControlFlowGraphReference? {
        if (function is FirDefaultPropertyAccessor) return null
        if (function is FirAnonymousFunction) {
            return exitAnonymousFunction(function)
        }
        // All non-lambda function are treated as concurrent since we do not make any assumption about when and how it's invoked.
        getOrCreateLocalVariableAssignmentAnalyzer(function)?.exitLocalFunction(function)

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
        } else {
            resetReceivers(graph.enterNode.flow)
        }
        return FirControlFlowGraphReferenceImpl(graph, DataFlowInfo(variableStorage, flowOnNodes))
    }

    // ----------------------------------- Anonymous function -----------------------------------

    private fun enterAnonymousFunction(anonymousFunction: FirAnonymousFunction) {
        getOrCreateLocalVariableAssignmentAnalyzer(anonymousFunction)?.apply {
            finishPostponedAnonymousFunction()
            enterLocalFunction(anonymousFunction)
        }
        val (postponedLambdaEnterNode, functionEnterNode) = graphBuilder.enterAnonymousFunction(anonymousFunction)
        postponedLambdaEnterNode?.mergeIncomingFlow()
        val flowOnEntry = functionEnterNode.mergeIncomingFlow()
        when (anonymousFunction.invocationKind) {
            EventOccurrencesRange.AT_LEAST_ONCE,
            EventOccurrencesRange.MORE_THAN_ONCE,
            EventOccurrencesRange.UNKNOWN, null ->
                enterCapturingStatement(functionEnterNode, anonymousFunction)
            else -> {}
        }
        resetReceivers(flowOnEntry)
    }

    private fun exitAnonymousFunction(anonymousFunction: FirAnonymousFunction): FirControlFlowGraphReference {
        getOrCreateLocalVariableAssignmentAnalyzer(anonymousFunction)?.exitLocalFunction(
            anonymousFunction
        )
        val (functionExitNode, postponedLambdaExitNode, graph) = graphBuilder.exitAnonymousFunction(anonymousFunction)
        when (anonymousFunction.invocationKind) {
            EventOccurrencesRange.AT_LEAST_ONCE,
            EventOccurrencesRange.MORE_THAN_ONCE,
            EventOccurrencesRange.UNKNOWN, null ->
                exitCapturingStatement(anonymousFunction)
            else -> {}
        }
        functionExitNode.mergeIncomingFlow()
        postponedLambdaExitNode?.mergeIncomingFlow()
        resetReceivers(graph.enterNode.flow)
        return FirControlFlowGraphReferenceImpl(graph)
    }

    fun visitPostponedAnonymousFunction(anonymousFunctionExpression: FirAnonymousFunctionExpression) {
        val anonymousFunction = anonymousFunctionExpression.anonymousFunction
        getOrCreateLocalVariableAssignmentAnalyzer(anonymousFunction)?.visitPostponedAnonymousFunction(anonymousFunction)
        val (enterNode, exitNode) = graphBuilder.visitPostponedAnonymousFunction(anonymousFunctionExpression)
        enterNode.mergeIncomingFlow()
        exitNode.mergeIncomingFlow()
    }

    fun exitAnonymousFunctionExpression(anonymousFunctionExpression: FirAnonymousFunctionExpression) {
        graphBuilder.exitAnonymousFunctionExpression(anonymousFunctionExpression).mergeIncomingFlow()
    }

    // ----------------------------------- Classes -----------------------------------

    fun enterClass() {
        graphBuilder.enterClass()
    }

    fun exitClass() {
        graphBuilder.exitClass()
    }

    fun exitRegularClass(klass: FirRegularClass): ControlFlowGraph {
        if (klass.isLocal && components.container !is FirClass) return exitLocalClass(klass)
        return graphBuilder.exitClass(klass)
    }

    private fun exitLocalClass(klass: FirRegularClass): ControlFlowGraph {
        // TODO: support capturing of mutable properties, KT-44877
        val (node, controlFlowGraph) = graphBuilder.exitLocalClass(klass)
        node.mergeIncomingFlow()
        return controlFlowGraph
    }

    fun enterAnonymousObject(anonymousObject: FirAnonymousObject) {
        graphBuilder.enterAnonymousObject(anonymousObject).mergeIncomingFlow()
    }

    fun exitAnonymousObject(anonymousObject: FirAnonymousObject): ControlFlowGraph {
        val (node, controlFlowGraph) = graphBuilder.exitAnonymousObject(anonymousObject)
        node.mergeIncomingFlow()
        return controlFlowGraph
    }

    fun exitAnonymousObjectExpression(anonymousObjectExpression: FirAnonymousObjectExpression) {
        graphBuilder.exitAnonymousObjectExpression(anonymousObjectExpression).mergeIncomingFlow()
    }

    // ----------------------------------- Value parameters (and it's defaults) -----------------------------------

    fun enterValueParameter(valueParameter: FirValueParameter) {
        graphBuilder.enterValueParameter(valueParameter)?.mergeIncomingFlow()
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

    // ----------------------------------- Field -----------------------------------

    fun enterField(field: FirField) {
        graphBuilder.enterField(field)?.mergeIncomingFlow()
    }

    fun exitField(field: FirField): ControlFlowGraph? {
        val (node, graph) = graphBuilder.exitField(field) ?: return null
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
        val node = graphBuilder.exitTypeOperatorCall(typeOperatorCall)
        val flow = node.mergeIncomingFlow()
        if (typeOperatorCall.operation !in FirOperation.TYPES) return
        val type = typeOperatorCall.conversionTypeRef.coneType
        val operandVariable = variableStorage.getOrCreateVariable(node.previousFlow, typeOperatorCall.argument)

        when (val operation = typeOperatorCall.operation) {
            FirOperation.IS, FirOperation.NOT_IS -> {
                val isType = operation == FirOperation.IS
                when (type) {
                    // x is Nothing? <=> x == null
                    nullableNothing -> processEqNull(node, typeOperatorCall.argument, isType)
                    // x is Any <=> x != null
                    any -> processEqNull(node, typeOperatorCall.argument, !isType)
                    else -> {
                        val expressionVariable = variableStorage.createSyntheticVariable(typeOperatorCall)
                        if (operandVariable.isReal()) {
                            flow.addImplication((expressionVariable eq isType) implies (operandVariable typeEq type))
                        }
                        if (!type.canBeNull) {
                            flow.addImplication((expressionVariable eq isType) implies (operandVariable notEq null))
                        }
                    }
                }
            }

            FirOperation.AS -> {
                if (operandVariable.isReal()) {
                    flow.addTypeStatement(operandVariable typeEq type)
                }
                if (!type.canBeNull) {
                    flow.commitOperationStatement(operandVariable notEq null, shouldRemoveSynthetics = true)
                } else {
                    val expressionVariable = variableStorage.createSyntheticVariable(typeOperatorCall)
                    flow.addImplication((expressionVariable notEq null) implies (operandVariable notEq null))
                }
            }

            FirOperation.SAFE_AS -> {
                val expressionVariable = variableStorage.createSyntheticVariable(typeOperatorCall)
                flow.addImplication((expressionVariable notEq null) implies (operandVariable notEq null))
                if (operandVariable.isReal()) {
                    flow.addImplication((expressionVariable notEq null) implies (operandVariable typeEq type))
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
        val node = graphBuilder.exitEqualityOperatorCall(equalityOperatorCall)
        node.mergeIncomingFlow()
        val operation = equalityOperatorCall.operation
        val leftOperand = equalityOperatorCall.arguments[0]
        val rightOperand = equalityOperatorCall.arguments[1]

        /*
         * This unwrapping is needed for cases like
         * when (true) {
         *     s != null -> s.length
         * }
         *
         * FirWhenSubjectExpression may be only in the lhs of equality operator call
         *   by how is FIR for when branches is built, so there is no need to unwrap
         *   right argument
         */
        val leftConst = when (leftOperand) {
            is FirWhenSubjectExpression -> leftOperand.whenRef.value.subject
            else -> leftOperand
        } as? FirConstExpression<*>
        val rightConst = rightOperand as? FirConstExpression<*>
        val leftIsNullConst = leftConst?.kind == ConstantValueKind.Null
        val rightIsNullConst = rightConst?.kind == ConstantValueKind.Null
        val leftIsNull = leftIsNullConst || leftOperand.coneType.isNullableNothing && !rightIsNullConst
        val rightIsNull = rightIsNullConst || rightOperand.coneType.isNullableNothing && !leftIsNullConst

        when {
            leftConst != null && rightConst != null -> return
            leftIsNull -> processEqNull(node, rightOperand, operation.isEq())
            rightIsNull -> processEqNull(node, leftOperand, operation.isEq())
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

        // propagating facts for (... == true) and (... == false)
        when (const.kind) {
            ConstantValueKind.Boolean -> {
                val constValue = const.value as Boolean
                val shouldInvert = isEq xor constValue

                logicSystem.translateVariableFromConditionInStatements(
                    flow,
                    operandVariable,
                    expressionVariable,
                    shouldRemoveOriginalStatements = operandVariable.isSynthetic()
                ) {
                    when (it.condition.operation) {
                        // Whatever the result is after comparing operandVariable with `true` or `false` cannot let you imply effects that apply
                        // when the operandVariable is null. Hence we return null here.
                        Operation.EqNull -> null
                        Operation.NotEqNull -> {
                            (expressionVariable eq isEq) implies (it.effect)
                        }
                        Operation.EqTrue, Operation.EqFalse -> {
                            if (shouldInvert) it.invertCondition() else it
                        }
                    }
                }
            }
            ConstantValueKind.Null -> {
                logicSystem.translateVariableFromConditionInStatements(
                    flow,
                    operandVariable,
                    expressionVariable,
                    shouldRemoveOriginalStatements = operandVariable.isSynthetic()
                ) {
                    when (it.condition.operation) {
                        Operation.EqNull -> (expressionVariable eq isEq) implies (it.effect)
                        Operation.NotEqNull -> (expressionVariable eq !isEq) implies (it.effect)
                        // Whatever the result is after comparing operandVariable with `null` cannot let you imply effects that apply when the
                        // operandVariable is true or false.
                        Operation.EqTrue, Operation.EqFalse -> null
                    }
                }
            }
            else -> {
                // Inconclusive if the user code compares with other constants.
            }
        }
    }

    private fun processEqNull(node: CFGNode<*>, operand: FirExpression, isEq: Boolean) {
        val flow = node.flow
        val expressionVariable = variableStorage.createSyntheticVariable(node.fir)
        val operandVariable = variableStorage.getOrCreateVariable(node.previousFlow, operand)
        flow.addImplication((expressionVariable eq isEq) implies (operandVariable eq null))
        flow.addImplication((expressionVariable notEq isEq) implies (operandVariable notEq null))
    }

    private fun processEq(
        node: EqualityOperatorCallNode,
        leftOperand: FirExpression,
        rightOperand: FirExpression,
        operation: FirOperation,
    ) {
        val isEq = operation.isEq()
        val leftOperandType = leftOperand.coneType
        val rightOperandType = rightOperand.coneType
        val leftIsNullable = leftOperandType.isMarkedNullable
        val rightIsNullable = rightOperandType.isMarkedNullable

        if (leftIsNullable && rightIsNullable) {
            // The logic system is not complex enough to express a second level of implications this creates:
            // if either `== null` then this creates the same implications as a constant null comparison,
            // otherwise the same as if the corresponding `...IsNullable` is false.
            return
        }

        val flow = node.flow
        val expressionVariable = variableStorage.createSyntheticVariable(node.fir)
        val leftOperandVariable = variableStorage.getOrCreateVariable(node.previousFlow, leftOperand)
        val rightOperandVariable = variableStorage.getOrCreateVariable(node.previousFlow, rightOperand)

        if (leftIsNullable || rightIsNullable) {
            // `a == b:Any` => `a != null`; the inverse is not true - we don't know when `a` *is* `null`
            val nullableOperand = if (leftIsNullable) leftOperandVariable else rightOperandVariable
            flow.addImplication((expressionVariable eq isEq) implies (nullableOperand notEq null))
        }

        if (!leftOperandVariable.isReal() && !rightOperandVariable.isReal()) return

        if (operation == FirOperation.EQ || operation == FirOperation.NOT_EQ) {
            if (hasOverriddenEquals(leftOperandType)) return
        }

        if (leftOperandVariable.isReal()) {
            flow.addImplication((expressionVariable eq isEq) implies (leftOperandVariable typeEq rightOperandType))
        }

        if (rightOperandVariable.isReal()) {
            flow.addImplication((expressionVariable eq isEq) implies (rightOperandVariable typeEq leftOperandType))
        }
    }

    private fun hasOverriddenEquals(type: ConeKotlinType): Boolean {
        val session = components.session
        val symbolsForType = collectSymbolsForType(type, session)
        if (symbolsForType.any { it.hasEqualsOverride(session, checkModality = true) }) return true

        val superTypes = lookupSuperTypes(
            symbolsForType,
            lookupInterfaces = false,
            deep = true,
            session,
            substituteTypes = false
        )
        val superClassSymbols = superTypes.mapNotNull {
            it.fullyExpandedType(session).toSymbol(session) as? FirRegularClassSymbol
        }

        return superClassSymbols.any { it.hasEqualsOverride(session, checkModality = false) }
    }

    private fun FirClassSymbol<*>.hasEqualsOverride(session: FirSession, checkModality: Boolean): Boolean {
        val status = resolvedStatus
        if (checkModality && status.modality != Modality.FINAL) return true
        if (status.isExpect) return true
        when (classId) {
            StandardClassIds.Any, StandardClassIds.String -> return false
            // Float and Double effectively had non-trivial `equals` semantics while they don't have explicit overrides (see KT-50535)
            StandardClassIds.Float, StandardClassIds.Double -> return true
        }

        // When the class belongs to a different module, "equals" contract might be changed without re-compilation
        // But since we had such behavior in FE1.0, it might be too strict to prohibit it now, especially once there's a lot of cases
        // when different modules belong to a single project, so they're totally safe (see KT-50534)
        // if (moduleData != session.moduleData) {
        //     return true
        // }

        return session.declaredMemberScope(this)
            .getFunctions(OperatorNameConventions.EQUALS)
            .any { it.fir.isEquals() }
    }

    // ----------------------------------- Jump -----------------------------------

    fun exitJump(jump: FirJump<*>) {
        graphBuilder.exitJump(jump).mergeIncomingFlow()
    }

    // ----------------------------------- Check not null call -----------------------------------

    fun exitCheckNotNullCall(checkNotNullCall: FirCheckNotNullCall, callCompleted: Boolean) {
        // Add `Any` to the set of possible types; the intersection type `T? & Any` will be reduced to `T` after smartcast.
        val (node, unionNode) = graphBuilder.exitCheckNotNullCall(checkNotNullCall, callCompleted)
        val argumentVariable = variableStorage.getOrCreateVariable(node.previousFlow, checkNotNullCall.argument)
        node.mergeIncomingFlow().commitOperationStatement(argumentVariable notEq null, shouldRemoveSynthetics = false)
        unionNode?.unionFlowFromArguments()
    }

    // ----------------------------------- When -----------------------------------

    fun enterWhenExpression(whenExpression: FirWhenExpression) {
        graphBuilder.enterWhenExpression(whenExpression).mergeIncomingFlow()
    }

    fun enterWhenBranchCondition(whenBranch: FirWhenBranch) {
        val node = graphBuilder.enterWhenBranchCondition(whenBranch)
        val flow = node.mergeIncomingFlow(updateReceivers = true)
        val previousNode = node.previousNodes.single()
        if (previousNode is WhenBranchConditionExitNode) {
            val conditionVariable = context.variablesForWhenConditions.remove(previousNode)!!
            flow.commitOperationStatement(conditionVariable eq false, shouldRemoveSynthetics = true)
        }
    }

    fun exitWhenBranchCondition(whenBranch: FirWhenBranch) {
        val (conditionExitNode, branchEnterNode) = graphBuilder.exitWhenBranchCondition(whenBranch)
        val conditionExitFlow = conditionExitNode.mergeIncomingFlow()
        val conditionVariable = variableStorage.getOrCreateVariable(conditionExitFlow, whenBranch.condition)
        context.variablesForWhenConditions[conditionExitNode] = conditionVariable
        branchEnterNode.flow = conditionExitFlow.fork().also {
            it.commitOperationStatement(conditionVariable eq true, shouldRemoveSynthetics = false)
        }
    }

    fun exitWhenBranchResult(whenBranch: FirWhenBranch) {
        graphBuilder.exitWhenBranchResult(whenBranch).mergeIncomingFlow()
    }

    fun exitWhenExpression(whenExpression: FirWhenExpression) {
        val (whenExitNode, syntheticElseNode, mergePostponedLambdaExitsNode) = graphBuilder.exitWhenExpression(whenExpression)
        if (syntheticElseNode != null) {
            val previousConditionExitNode = syntheticElseNode.firstPreviousNode as? WhenBranchConditionExitNode
            // previous node for syntheticElseNode can be not WhenBranchConditionExitNode in case of `when` without any branches
            // in that case there will be when enter or subject access node
            if (previousConditionExitNode != null) {
                val conditionVariable = context.variablesForWhenConditions.remove(previousConditionExitNode)!!
                syntheticElseNode.flow = previousConditionExitNode.flow.fork().also {
                    it.commitOperationStatement(conditionVariable eq false, shouldRemoveSynthetics = true)
                }
            } else {
                syntheticElseNode.mergeIncomingFlow()
            }
        }
        whenExitNode.mergeIncomingFlow()
        mergePostponedLambdaExitsNode?.mergeIncomingFlow()
    }

    fun exitWhenSubjectExpression(expression: FirWhenSubjectExpression) {
        graphBuilder.exitWhenSubjectExpression(expression).mergeIncomingFlow()
    }

    // ----------------------------------- While Loop -----------------------------------

    private fun exitCommonLoop(exitNode: LoopExitNode) {
        val singlePreviousNode = exitNode.previousNodes.singleOrNull { !it.isDead }
        if (singlePreviousNode is LoopConditionExitNode) {
            val variable = variableStorage.getOrCreateVariable(exitNode.previousFlow, singlePreviousNode.fir)
            exitNode.flow.commitOperationStatement(variable eq false, shouldRemoveSynthetics = true)
        }
        exitCapturingStatement(exitNode.fir)
    }

    fun enterWhileLoop(loop: FirLoop) {
        val (loopEnterNode, loopConditionEnterNode) = graphBuilder.enterWhileLoop(loop)
        loopEnterNode.mergeIncomingFlow()
        enterCapturingStatement(loopEnterNode, loop)
        loopConditionEnterNode.mergeIncomingFlow()
    }

    fun exitWhileLoopCondition(loop: FirLoop) {
        val (loopConditionExitNode, loopBlockEnterNode) = graphBuilder.exitWhileLoopCondition(loop)
        val conditionExitFlow = loopConditionExitNode.mergeIncomingFlow()
        loopBlockEnterNode.flow = conditionExitFlow.fork().also {
            val conditionVariable = variableStorage.getVariable(conditionExitFlow, loop.condition)
            if (conditionVariable != null) {
                it.commitOperationStatement(conditionVariable eq true, shouldRemoveSynthetics = false)
            }
        }
    }

    fun exitWhileLoop(loop: FirLoop) {
        val (blockExitNode, exitNode) = graphBuilder.exitWhileLoop(loop)
        blockExitNode.mergeIncomingFlow()
        exitNode.mergeIncomingFlow()
        exitCommonLoop(exitNode)
    }

    private fun enterCapturingStatement(node: CFGNode<*>, statement: FirStatement) {
        val reassignedNames = context.preliminaryLoopVisitor.enterCapturingStatement(statement)
        if (reassignedNames.isEmpty()) return
        val possiblyChangedVariables = variableStorage.realVariables.filterKeys {
            val fir = (it.symbol as? FirVariableSymbol<*>)?.fir ?: return@filterKeys false
            fir.isVar && fir.name in reassignedNames
        }.values
        if (possiblyChangedVariables.isEmpty()) return
        val flow = node.flow
        for (variable in possiblyChangedVariables) {
            logicSystem.removeAllAboutVariable(flow, variable)
        }
    }

    private fun exitCapturingStatement(statement: FirStatement) {
        context.preliminaryLoopVisitor.exitCapturingStatement(statement)
    }

    // ----------------------------------- Do while Loop -----------------------------------

    fun enterDoWhileLoop(loop: FirLoop) {
        val (loopEnterNode, loopBlockEnterNode) = graphBuilder.enterDoWhileLoop(loop)
        loopEnterNode.mergeIncomingFlow()
        enterCapturingStatement(loopEnterNode, loop)
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

    fun exitTryMainBlock() {
        graphBuilder.exitTryMainBlock().mergeIncomingFlow()
    }

    fun enterCatchClause(catch: FirCatch) {
        graphBuilder.enterCatchClause(catch).mergeIncomingFlow(updateReceivers = true)
    }

    fun exitCatchClause(catch: FirCatch) {
        graphBuilder.exitCatchClause(catch).mergeIncomingFlow()
    }

    fun enterFinallyBlock() {
        graphBuilder.enterFinallyBlock().mergeIncomingFlow(updateReceivers = true)
    }

    fun exitFinallyBlock() {
        graphBuilder.exitFinallyBlock().mergeIncomingFlow()
    }

    fun exitTryExpression(callCompleted: Boolean) {
        val (tryExpressionExitNode, unionNode) = graphBuilder.exitTryExpression(callCompleted)
        tryExpressionExitNode.mergeIncomingFlow()
        unionNode?.unionFlowFromArguments()
    }

    // ----------------------------------- Resolvable call -----------------------------------

    // Intentionally left empty for potential future needs (call sites are preserved)
    fun enterQualifiedAccessExpression() {}

    fun exitQualifiedAccessExpression(qualifiedAccessExpression: FirQualifiedAccessExpression) {
        graphBuilder.exitQualifiedAccessExpression(qualifiedAccessExpression).mergeIncomingFlow()
        processConditionalContract(qualifiedAccessExpression)
    }

    fun exitSmartCastExpression(smartCastExpression: FirSmartCastExpression) {
        graphBuilder.exitSmartCastExpression(smartCastExpression).mergeIncomingFlow()
    }

    fun enterSafeCallAfterNullCheck(safeCall: FirSafeCallExpression) {
        val node = graphBuilder.enterSafeCall(safeCall)
        val flow = node.mergeIncomingFlow()
        // When calling `c` in `a?.b?.c`, all type information obtained after calling `b` is valid as we know `a`
        // is non-null. In theory, this should be unnecessary if the TODOs below are implemented.
        val flowFromPreviousSafeCall = (node.firstPreviousNode as? ExitSafeCallNode)?.lastNodeInNotNullCase?.flow
        if (flowFromPreviousSafeCall != null) {
            logicSystem.copyAllInformation(flowFromPreviousSafeCall, flow)
        }
        val receiverVariable = variableStorage.getOrCreateVariable(node.flow, safeCall.receiver)
        flow.commitOperationStatement(receiverVariable notEq null, shouldRemoveSynthetics = true)
    }

    fun exitSafeCall(safeCall: FirSafeCallExpression) {
        val (node, mergePostponedLambdaExitsNode) = graphBuilder.exitSafeCall()
        val flow = node.mergeIncomingFlow()
        mergePostponedLambdaExitsNode?.mergeIncomingFlow()

        val variable = variableStorage.getOrCreateVariable(flow, safeCall)
        val receiverVariable = variableStorage.getOrCreateVariable(flow, safeCall.receiver)
        // TODO? if the callee has non-null return type, then (variable eq null) implies (receiverVariable eq null)
        //   if (x?.toString() == null) { /* x == null */ }
        // TODO? all new statements in previous node's flow are valid here if receiverVariable != null
        //   if (x?.whatever(y as String) != null) { /* y is String */ }
        // TODO? all new implications in previous node's flow are valid here if receiverVariable != null
        //  (that requires a second level of implications: receiverVariable != null => condition => effect).
        flow.addImplication((variable notEq null) implies (receiverVariable notEq null))
    }

    fun exitResolvedQualifierNode(resolvedQualifier: FirResolvedQualifier) {
        graphBuilder.exitResolvedQualifierNode(resolvedQualifier).mergeIncomingFlow()
    }

    fun enterCall() {
        graphBuilder.enterCall()
    }

    private tailrec fun FirExpression.getAnonymousFunction(): FirAnonymousFunction? = when (this) {
        is FirAnonymousFunctionExpression -> anonymousFunction
        is FirLambdaArgumentExpression -> expression.getAnonymousFunction()
        else -> null
    }

    private var functionCallLevel = 0

    fun enterFunctionCall(functionCall: FirFunctionCall) {
        val lambdaArgs = functionCall.arguments.mapNotNullTo(mutableSetOf()) { it.getAnonymousFunction() }
        val localVariableAssignmentAnalyzer = context.firLocalVariableAssignmentAnalyzer
            ?: if (lambdaArgs.isNotEmpty()) getOrCreateLocalVariableAssignmentAnalyzer(lambdaArgs.first()) else null
        localVariableAssignmentAnalyzer?.enterFunctionCall(lambdaArgs, functionCallLevel)
        functionCallLevel++
    }

    @OptIn(PrivateForInline::class)
    fun exitFunctionCall(functionCall: FirFunctionCall, callCompleted: Boolean) {
        functionCallLevel--
        context.firLocalVariableAssignmentAnalyzer?.exitFunctionCall(callCompleted)
        if (ignoreFunctionCalls) {
            graphBuilder.exitIgnoredCall(functionCall)
            return
        }
        val (functionCallNode, unionNode) = graphBuilder.exitFunctionCall(functionCall, callCompleted)
        unionNode?.unionFlowFromArguments()
        functionCallNode.mergeIncomingFlow()
        if (functionCall.isBooleanNot()) {
            exitBooleanNot(functionCall, functionCallNode)
        }
        processConditionalContract(functionCall)
    }

    fun exitDelegatedConstructorCall(call: FirDelegatedConstructorCall, callCompleted: Boolean) {
        val (callNode, unionNode) = graphBuilder.exitDelegatedConstructorCall(call, callCompleted)
        unionNode?.unionFlowFromArguments()
        callNode.mergeIncomingFlow()
    }

    fun exitStringConcatenationCall(call: FirStringConcatenationCall) {
        val (callNode, unionNode) = graphBuilder.exitStringConcatenationCall(call)
        unionNode?.unionFlowFromArguments()
        callNode.mergeIncomingFlow()
    }

    private fun UnionFunctionCallArgumentsNode.unionFlowFromArguments() {
        flow = logicSystem.unionFlow(previousNodes.map { it.flow }).also {
            resetReceivers(it)
        }
    }

    private fun processConditionalContract(qualifiedAccess: FirQualifiedAccess) {
        val owner: FirContractDescriptionOwner? = when (qualifiedAccess) {
            is FirFunctionCall -> qualifiedAccess.toResolvedCallableSymbol()?.fir as? FirSimpleFunction
            is FirQualifiedAccessExpression -> {
                val property = qualifiedAccess.calleeReference.resolvedSymbol?.fir as? FirProperty
                property?.getter
            }
            is FirVariableAssignment -> {
                val property = qualifiedAccess.lValue.resolvedSymbol?.fir as? FirProperty
                property?.setter
            }
            else -> null
        }

        val contractDescription = owner?.contractDescription as? FirResolvedContractDescription ?: return
        val conditionalEffects = contractDescription.effects.map { it.effect }.filterIsInstance<ConeConditionalEffectDeclaration>()
        if (conditionalEffects.isEmpty()) return
        val argumentsMapping = createArgumentsMapping(qualifiedAccess) ?: return

        val typeParameters = (owner as? FirTypeParameterRefsOwner)?.typeParameters
        val substitutor = if (!typeParameters.isNullOrEmpty()) {
            @Suppress("UNCHECKED_CAST")
            val substitutionFromArguments = typeParameters.zip(qualifiedAccess.typeArguments).map { (typeParameterRef, typeArgument) ->
                typeParameterRef.symbol to typeArgument.toConeTypeProjection().type
            }.filter { it.second != null }.toMap() as Map<FirTypeParameterSymbol, ConeKotlinType>
            ConeSubstitutorByMap(substitutionFromArguments, components.session)
        } else {
            ConeSubstitutor.Empty
        }

        contractDescriptionVisitingMode = true
        graphBuilder.enterContract(qualifiedAccess).mergeIncomingFlow()
        val lastFlow = graphBuilder.lastNode.flow
        val functionCallVariable = variableStorage.getOrCreateVariable(lastFlow, qualifiedAccess)
        for (conditionalEffect in conditionalEffects) {
            val fir = conditionalEffect.buildContractFir(argumentsMapping, substitutor) ?: continue
            val effect = conditionalEffect.effect as? ConeReturnsEffectDeclaration ?: continue
            fir.transformSingle(components.transformer, ResolutionMode.ContextDependent)
            val argumentVariable = variableStorage.getOrCreateVariable(lastFlow, fir)
            val lastNode = graphBuilder.lastNode
            when (val value = effect.value) {
                ConeConstantReference.WILDCARD -> {
                    lastNode.flow.commitOperationStatement(argumentVariable eq true, shouldRemoveSynthetics = true)
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
        graphBuilder.exitConstExpression(constExpression).mergeIncomingFlow()
    }

    fun exitLocalVariableDeclaration(variable: FirProperty, hadExplicitType: Boolean) {
        val flow = graphBuilder.exitVariableDeclaration(variable).mergeIncomingFlow()
        val initializer = variable.initializer ?: return
        exitVariableInitialization(flow, initializer, variable, assignment = null, hadExplicitType)
    }

    fun exitVariableAssignment(assignment: FirVariableAssignment) {
        val flow = graphBuilder.exitVariableAssignment(assignment).mergeIncomingFlow()
        val property = assignment.lValue.resolvedSymbol?.fir as? FirProperty ?: return
        if (property.isLocal || property.isVal) {
            exitVariableInitialization(flow, assignment.rValue, property, assignment, hasExplicitType = false)
        } else {
            // TODO: add unstable smartcast for non-local var
            val variable = variableStorage.getRealVariableWithoutUnwrappingAlias(flow, property.symbol, assignment)
            if (variable != null) {
                logicSystem.recordNewAssignment(flow, variable, context.newAssignmentIndex())
            }
        }
        processConditionalContract(assignment)
    }

    private fun exitVariableInitialization(
        flow: FLOW,
        initializer: FirExpression,
        property: FirProperty,
        assignment: FirVariableAssignment?,
        hasExplicitType: Boolean,
    ) {
        val propertyVariable = variableStorage.getOrCreateRealVariableWithoutUnwrappingAlias(
            flow,
            property.symbol,
            assignment ?: property,
            if (property.isVal) PropertyStability.STABLE_VALUE else PropertyStability.LOCAL_VAR
        )
        val isAssignment = assignment != null
        if (isAssignment) {
            logicSystem.recordNewAssignment(flow, propertyVariable, context.newAssignmentIndex())
        }

        variableStorage.getOrCreateRealVariable(flow, initializer.symbol, initializer.unwrapSmartcastExpression())
            ?.let { initializerVariable ->
                val isInitializerStable =
                    initializerVariable.isStable || (initializerVariable.hasLocalStability && initializer.isAccessToStableVariable())

                if (!hasExplicitType && isInitializerStable && (propertyVariable.hasLocalStability || propertyVariable.isStable)) {
                    logicSystem.addLocalVariableAlias(flow, propertyVariable, initializerVariable)
                } else {
                    logicSystem.replaceVariableFromConditionInStatements(flow, initializerVariable, propertyVariable)
                }
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
            // `propertyVariable` can be an alias to `initializerVariable`, in which case this will add
            // a redundant type statement which is fine...probably. TODO: store initial type within the variable?
            flow.addTypeStatement(flow.unwrapVariable(propertyVariable) typeEq initializer.typeRef.coneType)
        }
    }

    private fun FirExpression.isAccessToStableVariable(): Boolean =
        !isAccessToUnstableLocalVariable(this)

    private val RealVariable.isStable get() = stability == PropertyStability.STABLE_VALUE
    private val RealVariable.hasLocalStability get() = stability == PropertyStability.LOCAL_VAR


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
        val leftOperandVariable = variableStorage.getOrCreateVariable(parentFlow, leftNode.firstPreviousNode.fir)
        leftNode.flow = parentFlow.fork()
        rightNode.flow = parentFlow.fork().also {
            it.commitOperationStatement(leftOperandVariable eq isAnd, shouldRemoveSynthetics = false)
        }
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
        val flow = node.mergeIncomingFlow()

        /*
         * TODO: Here we should handle case when one of arguments is dead (e.g. in cases `false && expr` or `true || expr`)
         *  But since conditions with const are rare it can be delayed
         */
        val leftVariable = variableStorage.getOrCreateVariable(flow, binaryLogicExpression.leftOperand)
        val rightVariable = variableStorage.getOrCreateVariable(flow, binaryLogicExpression.rightOperand)
        val operatorVariable = variableStorage.getOrCreateVariable(flow, binaryLogicExpression)

        if (!node.leftOperandNode.isDead && node.rightOperandNode.isDead) {
            // If the right operand does not terminate, then we know that the value of the entire expression
            // has to be `onlyLeftEvaluated`, and it has to be produced by the left operand.
            flow.commitOperationStatement(leftVariable eq onlyLeftEvaluated, shouldRemoveSynthetics = true)
        } else {
            // If `left && right` is true, then both are true (and evaluated).
            // If `left || right` is false, then both are false.
            arrayOf(
                flowFromRight.approvedTypeStatements,
                // `leftVariable eq bothEvaluated` already approved in flowFromRight.
                logicSystem.approveOperationStatement(flowFromRight, rightVariable eq bothEvaluated),
            ).forEach { statements ->
                statements.values.forEach { flow.addImplication((operatorVariable eq bothEvaluated) implies it) }
            }

            // If `left && right` is false, then either `left` is false, or both were evaluated and `right` is false.
            // If `left || right` is true, then either `left` is true, or both were evaluated and `right` is true.
            logicSystem.orForTypeStatements(
                logicSystem.approveOperationStatement(flowFromLeft, leftVariable eq onlyLeftEvaluated),
                // TODO: and(approved from right, ...)? FE1.0 doesn't seem to handle that correctly either.
                //   if (x is A || whatever(x as B)) { /* x is (A | B) */ }
                logicSystem.approveOperationStatement(flowFromRight, rightVariable eq onlyLeftEvaluated),
            ).values.forEach { flow.addImplication((operatorVariable eq onlyLeftEvaluated) implies it) }
        }

        resetReceivers(flow)
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

    fun enterAnnotation(annotation: FirAnnotation) {
        graphBuilder.enterAnnotation(annotation).mergeIncomingFlow()
    }

    fun exitAnnotation(annotation: FirAnnotation) {
        graphBuilder.exitAnnotation(annotation).mergeIncomingFlow()
    }

    // ----------------------------------- Init block -----------------------------------

    fun enterInitBlock(initBlock: FirAnonymousInitializer) {
        graphBuilder.enterInitBlock(initBlock).let { (node, prevNode) ->
            if (prevNode != null) {
                node.flow = prevNode.flow.fork()
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

    fun enterElvis(elvisExpression: FirElvisExpression) {
        graphBuilder.enterElvis(elvisExpression)
    }

    fun exitElvisLhs(elvisExpression: FirElvisExpression) {
        val (lhsExitNode, lhsIsNotNullNode, rhsEnterNode) = graphBuilder.exitElvisLhs(elvisExpression)
        val flow = lhsExitNode.mergeIncomingFlow()
        val lhsVariable = variableStorage.getOrCreateVariable(flow, elvisExpression.lhs)
        lhsIsNotNullNode.flow = flow.fork().also {
            it.commitOperationStatement(lhsVariable notEq null, shouldRemoveSynthetics = false)
        }
        rhsEnterNode.flow = flow.fork().also {
            it.commitOperationStatement(lhsVariable eq null, shouldRemoveSynthetics = false)
            resetReceivers(it)
        }
    }

    fun exitElvis(elvisExpression: FirElvisExpression, isLhsNotNull: Boolean) {
        val (node, mergePostponedLambdaExitsNode) = graphBuilder.exitElvis()
        val flow = node.mergeIncomingFlow()
        mergePostponedLambdaExitsNode?.mergeIncomingFlow()
        if (isLhsNotNull) {
            val lhsVariable = variableStorage.getOrCreateVariable(node.previousFlow, elvisExpression.lhs)
            flow.commitOperationStatement(lhsVariable notEq null, shouldRemoveSynthetics = true)
        }

        if (!components.session.languageVersionSettings.supportsFeature(LanguageFeature.BooleanElvisBoundSmartCasts)) return
        val rhs = elvisExpression.rhs
        if (rhs is FirConstExpression<*> && rhs.kind == ConstantValueKind.Boolean) {
            val lhs = elvisExpression.lhs
            if (lhs.typeRef.coneType.classId != StandardClassIds.Boolean) return

            // a ?: false == true -> a != null
            // a ?: true == false -> a != null
            val elvisVariable = variableStorage.getOrCreateVariable(flow, elvisExpression)
            val lhsVariable = variableStorage.getOrCreateVariable(flow, lhs)

            val value = rhs.value as Boolean
            flow.addImplication((elvisVariable eq !value) implies (lhsVariable notEq null))
        }
    }

    // Callable reference

    fun exitCallableReference(callableReferenceAccess: FirCallableReferenceAccess) {
        graphBuilder.exitCallableReference(callableReferenceAccess).mergeIncomingFlow()
    }

    fun exitGetClassCall(getClassCall: FirGetClassCall) {
        graphBuilder.exitGetClassCall(getClassCall).mergeIncomingFlow()
    }

    // ------------------------------------------------------ Utils ------------------------------------------------------

    private fun getOrCreateLocalVariableAssignmentAnalyzer(firFunction: FirFunction): FirLocalVariableAssignmentAnalyzer? {
        // Only return analyzer for nested functions so that we won't waste time on functions that don't contain any lambda or local
        // function.
        val rootFunction = components.containingDeclarations.firstIsInstanceOrNull<FirFunction>() ?: return null
        if (rootFunction == firFunction) return null
        return context.firLocalVariableAssignmentAnalyzer ?: FirLocalVariableAssignmentAnalyzer.analyzeFunction(rootFunction).also {
            context.firLocalVariableAssignmentAnalyzer = it
        }
    }

    private var CFGNode<*>.flow: FLOW
        get() = context.flowOnNodes.getValue(this.origin)
        set(value) {
            context.flowOnNodes[this.origin] = value
        }

    private val CFGNode<*>.origin: CFGNode<*> get() = if (this is StubNode) firstPreviousNode else this

    private fun CFGNode<*>.mergeIncomingFlow(
        // This flag should be set true if we're changing flow branches from one to another (e.g. in when, try->catch)
        updateReceivers: Boolean = false
    ): FLOW {
        val previousFlows = mutableListOf<FLOW>()
        var deadForwardCount = 0
        for (previousNode in previousNodes) {
            val incomingEdgeKind = incomingEdges.getValue(previousNode).kind
            if (incomingEdgeKind == EdgeKind.DeadForward) {
                deadForwardCount++
            }
            if (incomingEdgeKind.usedInDfa || (isDead && incomingEdgeKind.usedInDeadDfa)) {
                previousFlows += previousNode.flow
            }
        }
        return logicSystem.joinFlow(previousFlows).also {
            flow = it
            if (updateReceivers || previousFlows.size + deadForwardCount > 1) {
                resetReceivers(it)
            }
        }
    }

    private fun resetReceivers(flow: FLOW) {
        receiverStack.forEach {
            variableStorage.getRealVariable(flow, it.boundSymbol, it.receiverExpression)?.let { variable ->
                receiverUpdated(it.boundSymbol, flow.getType(variable))
            }
        }
    }

    private fun FLOW.addImplication(statement: Implication) {
        val effect = statement.effect
        if (effect is OperationStatement) {
            val variable = effect.variable
            if (variable.isReal()) {
                when (effect.operation) {
                    Operation.EqNull -> variable typeEq nullableNothing
                    Operation.NotEqNull -> variable typeEq any
                    else -> null
                }?.let { logicSystem.addImplication(this, statement.condition implies it) }
            }
        }
        logicSystem.addImplication(this, statement)
    }

    private fun FLOW.addTypeStatement(info: TypeStatement) {
        logicSystem.addTypeStatement(this, info)
        if (info.variable.isThisReference) {
            receiverUpdated(info.variable.identifier.symbol, getType(info.variable))
        }
    }

    private fun FLOW.fork(): FLOW =
        logicSystem.forkFlow(this)

    private fun FLOW.commitOperationStatement(statement: OperationStatement, shouldRemoveSynthetics: Boolean) {
        logicSystem.approveOperationStatement(this, statement, shouldRemoveSynthetics).values.forEach {
            addTypeStatement(it)
        }
        if (statement.operation == Operation.NotEqNull) {
            val variable = statement.variable
            if (variable is RealVariable) {
                addTypeStatement(variable typeEq any)
            }
        }
    }

    private val CFGNode<*>.previousFlow: FLOW
        get() = firstPreviousNode.flow
}
