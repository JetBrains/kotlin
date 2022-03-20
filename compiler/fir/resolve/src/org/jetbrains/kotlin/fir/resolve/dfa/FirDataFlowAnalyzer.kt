/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.dfa

import org.jetbrains.kotlin.config.LanguageFeature
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
import org.jetbrains.kotlin.utils.addIfNotNull
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
                private val receiverStack: PersistentImplicitReceiverStack
                    get() = components.implicitReceiverStack as PersistentImplicitReceiverStack

                private val visibilityChecker = components.session.visibilityChecker

                override val logicSystem: PersistentLogicSystem =
                    object : PersistentLogicSystem(components.session.typeContext) {
                        override fun processUpdatedReceiverVariable(flow: PersistentFlow, variable: RealVariable) {
                            val symbol = variable.identifier.symbol

                            val index = receiverStack.getReceiverIndex(symbol) ?: return
                            val info = flow.getTypeStatement(variable)

                            val type = if (info == null) {
                                receiverStack.getOriginalType(index)
                            } else {
                                val types = info.exactType.toMutableList().also {
                                    it += receiverStack.getOriginalType(index)
                                }
                                context.intersectTypesOrNull(types)!!
                            }
                            receiverStack.replaceReceiverType(index, type)
                        }

                        override fun updateAllReceivers(flow: PersistentFlow) {
                            receiverStack.forEach {
                                variableStorage.getRealVariable(flow, it.boundSymbol, it.receiverExpression)?.let { variable ->
                                    processUpdatedReceiverVariable(flow, variable)
                                }
                            }
                        }

                        override fun ConeKotlinType.isAcceptableForSmartcast(): Boolean {
                            if (this.isNullableNothing) return false
                            return when (this) {
                                is ConeClassLikeType -> {
                                    val symbol =
                                        fullyExpandedType(components.session).lookupTag.toSymbol(components.session) ?: return false
                                    val declaration = symbol.fir as? FirRegularClass ?: return true
                                    visibilityChecker.isVisible(
                                        declaration,
                                        components.session,
                                        components.context.file,
                                        components.context.containers,
                                        dispatchReceiver = null
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

    private val graphBuilder get() = context.graphBuilder
    protected val variableStorage get() = context.variableStorage

    private var contractDescriptionVisitingMode = false

    private val any = components.session.builtinTypes.anyType.type
    private val nullableNothing = components.session.builtinTypes.nullableNothingType.type

    @PrivateForInline
    var ignoreFunctionCalls: Boolean = false

    // ----------------------------------- Requests -----------------------------------

    fun isAccessToUnstableLocalVariable(expression: FirExpression): Boolean {
        val qualifiedAccessExpression = when (expression) {
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
        var variable = variableStorage.getRealVariableWithoutUnwrappingAlias(flow, symbol, expression) ?: return null
        val stability = variable.stability
        val result = mutableListOf<ConeKotlinType>()
        flow.directAliasMap[variable]?.let {
            result.addIfNotNull(it.originalType)
            variable = it.variable
        }
        flow.getTypeStatement(variable)?.exactType?.let { result += it }
        return result.takeIf { it.isNotEmpty() }?.let { stability to it }
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

        val (functionEnterNode, localFunctionNode, previousNode) = graphBuilder.enterFunction(function)
        localFunctionNode?.mergeIncomingFlow()
        functionEnterNode.mergeIncomingFlow(shouldForkFlow = previousNode != null)
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
        // TODO: questionable
        postponedLambdaEnterNode?.mergeIncomingFlow()
        functionEnterNode.mergeIncomingFlow()
        logicSystem.updateAllReceivers(functionEnterNode.flow)
    }

    private fun exitAnonymousFunction(anonymousFunction: FirAnonymousFunction): FirControlFlowGraphReference {
        getOrCreateLocalVariableAssignmentAnalyzer(anonymousFunction)?.exitLocalFunction(
            anonymousFunction
        )
        val (functionExitNode, postponedLambdaExitNode, graph) = graphBuilder.exitAnonymousFunction(anonymousFunction)
        // TODO: questionable
        postponedLambdaExitNode?.mergeIncomingFlow()
        functionExitNode.mergeIncomingFlow()
        return FirControlFlowGraphReferenceImpl(graph)
    }

    fun visitPostponedAnonymousFunction(anonymousFunctionExpression: FirAnonymousFunctionExpression) {
        val anonymousFunction = anonymousFunctionExpression.anonymousFunction
        getOrCreateLocalVariableAssignmentAnalyzer(anonymousFunction)?.visitPostponedAnonymousFunction(anonymousFunction)
        val (enterNode, exitNode) = graphBuilder.visitPostponedAnonymousFunction(anonymousFunctionExpression)
        enterNode.mergeIncomingFlow()
        exitNode.mergeIncomingFlow()
        enterNode.flow = enterNode.flow.fork()
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
        // TODO: support capturing of mutable properties, KT-44877
        val (node, controlFlowGraph) = graphBuilder.exitAnonymousObject(anonymousObject)
        node.mergeIncomingFlow()
        return controlFlowGraph
    }

    fun exitAnonymousObjectExpression(anonymousObjectExpression: FirAnonymousObjectExpression) {
        graphBuilder.exitAnonymousObjectExpression(anonymousObjectExpression).mergeIncomingFlow()
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
        val node = graphBuilder.exitTypeOperatorCall(typeOperatorCall).mergeIncomingFlow()
        if (typeOperatorCall.operation !in FirOperation.TYPES) return
        val type = typeOperatorCall.conversionTypeRef.coneType
        val operandVariable = variableStorage.getOrCreateVariable(node.previousFlow, typeOperatorCall.argument)
        val flow = node.flow

        when (val operation = typeOperatorCall.operation) {
            FirOperation.IS, FirOperation.NOT_IS -> {
                val expressionVariable = variableStorage.createSyntheticVariable(typeOperatorCall)
                val isNotNullCheck = !type.canBeNull
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
            leftIsNull -> processEqNull(node, rightOperand, operation)
            rightIsNull -> processEqNull(node, leftOperand, operation)
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
        if (operandVariable.isReal()) {
            flow.addImplication((expressionVariable eq isEq) implies (operandVariable typeEq any))
        }

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
                            if (shouldInvert) (it.condition.invert()) implies (it.effect)
                            else it
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

    private fun processEq(
        node: EqualityOperatorCallNode, leftOperand: FirExpression, rightOperand: FirExpression, operation: FirOperation
    ) {
        val leftIsNullable = leftOperand.coneType.isMarkedNullable
        val rightIsNullable = rightOperand.coneType.isMarkedNullable

        if (leftIsNullable || rightIsNullable) {
            if (leftIsNullable && rightIsNullable) return
            processEqNull(
                node,
                if (leftIsNullable) leftOperand else rightOperand,
                operation.invert(),
                checkAddImplicationForStatement = true
            )
        }

        processPossibleIdentity(node, leftOperand, rightOperand, operation)
    }

    /*
     * Process x == null in general: add implications for both cases.
     * E.g., say d1 is an eq operator call node, d2 represents `x`, the variable inside the operator call. So, d1: x == null
     * This util adds: "d1 == True -> d2 == null" and "d1 == False -> d2 != null"
     * so that both branches after the operator call can infer the type of x.
     *
     * However, users can specify what conditions are of interest.
     * E.g., say left == right _and_ right != null, then we can conclude left != null.
     * In this example, say d1 is an eq operator call (left == right), and d2 is left.
     * Unlike general cases, we want to add: "d1 == True -> d2 != null", and nothing more because the counter part,
     * "d1 == False -> d2 == null" doesn't hold. That is, left != right and right != null don't mean left == null. It just means, left is
     * something different from right, including null. By filtering "d1 == True" condition only, all the remaining logic can be shared.
     */
    private fun processEqNull(
        node: EqualityOperatorCallNode,
        operand: FirExpression,
        operation: FirOperation,
        checkAddImplicationForStatement: Boolean = false
    ) {
        val flow = node.flow
        val expressionVariable = variableStorage.createSyntheticVariable(node.fir)
        val operandVariable = variableStorage.getOrCreateVariable(node.previousFlow, operand)

        val isEq = operation.isEq()

        val predicate = when (isEq) {
            true -> operandVariable eq null
            false -> operandVariable notEq null
        }

        // left == right && right not null -> left != null
        // [processEqNull] adds both implications: operator call could be true or false. We definitely need the matched case only.
        fun shouldAddImplicationForStatement(operationStatement: OperationStatement): Boolean {
            if (!checkAddImplicationForStatement) return true
            // Only if operation statement is == True, i.e., left == right
            val operationStatementOp = operationStatement.operation
            return !isEq && operationStatementOp == Operation.EqTrue || isEq && operationStatementOp == Operation.EqFalse
        }

        logicSystem.approveOperationStatement(flow, predicate).forEach { effect ->
            if (shouldAddImplicationForStatement(expressionVariable eq true)) {
                flow.addImplication((expressionVariable eq true) implies effect)
            }
            if (shouldAddImplicationForStatement(expressionVariable eq false)) {
                flow.addImplication((expressionVariable eq false) implies effect.invert())
            }
        }

        val expressionVariableIsEq = shouldAddImplicationForStatement(expressionVariable eq isEq)
        val expressionVariableIsNotEq = shouldAddImplicationForStatement(expressionVariable notEq isEq)

        if (expressionVariableIsEq) {
            flow.addImplication((expressionVariable eq isEq) implies (operandVariable eq null))
        }
        if (expressionVariableIsNotEq) {
            flow.addImplication((expressionVariable notEq isEq) implies (operandVariable notEq null))
        }

        if (operandVariable.isReal()) {
            if (expressionVariableIsEq) {
                flow.addImplication((expressionVariable eq isEq) implies (operandVariable typeNotEq any))
            }
            if (expressionVariableIsNotEq) {
                flow.addImplication((expressionVariable notEq isEq) implies (operandVariable typeEq any))
            }

            if (shouldAddImplicationForStatement(expressionVariable eq !isEq)) {
                flow.addImplication((expressionVariable eq !isEq) implies (operandVariable typeNotEq nullableNothing))
            }
            if (shouldAddImplicationForStatement(expressionVariable notEq !isEq)) {
                flow.addImplication((expressionVariable notEq !isEq) implies (operandVariable typeEq nullableNothing))
            }
        }

        node.flow = flow
    }

    private fun processPossibleIdentity(
        node: EqualityOperatorCallNode,
        leftOperand: FirExpression,
        rightOperand: FirExpression,
        operation: FirOperation,
    ) {
        val flow = node.flow
        val expressionVariable = variableStorage.getOrCreateVariable(node.previousFlow, node.fir)
        val leftOperandVariable = variableStorage.getOrCreateVariable(node.previousFlow, leftOperand)
        val rightOperandVariable = variableStorage.getOrCreateVariable(node.previousFlow, rightOperand)
        val leftOperandType = leftOperand.coneType
        val rightOperandType = rightOperand.coneType

        if (!leftOperandVariable.isReal() && !rightOperandVariable.isReal()) return

        if (operation == FirOperation.EQ || operation == FirOperation.NOT_EQ) {
            if (hasOverriddenEquals(leftOperandType)) return
        }

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
        node.mergeIncomingFlow()

        fun FirExpression.propagateNotNullInfo() {
            val symbol = this.symbol
            if (symbol != null) {
                variableStorage.getOrCreateRealVariable(node.previousFlow, symbol, this)?.let { operandVariable ->
                    node.flow.addTypeStatement(operandVariable typeEq any)
                    logicSystem.approveStatementsInsideFlow(
                        node.flow,
                        operandVariable notEq null,
                        shouldRemoveSynthetics = true,
                        shouldForkFlow = false
                    )
                }
            }
            when (this) {
                is FirSafeCallExpression -> receiver.propagateNotNullInfo()
                is FirTypeOperatorCall -> {
                    if (operation == FirOperation.AS || operation == FirOperation.SAFE_AS) {
                        argument.propagateNotNullInfo()
                    }
                }
            }
        }

        checkNotNullCall.argument.propagateNotNullInfo()

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
        whenExitNode.mergeIncomingFlow()
    }

    fun exitWhenSubjectExpression(expression: FirWhenSubjectExpression) {
        graphBuilder.exitWhenSubjectExpression(expression).mergeIncomingFlow()
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
        loopConditionExitNode.mergeIncomingFlow()
        val conditionExitFlow = loopConditionExitNode.flow
        loopBlockEnterNode.flow = variableStorage.getVariable(conditionExitFlow, loop.condition)?.let { conditionVariable ->
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
        // NB: fork to isolate effects inside the try main block
        // Otherwise, changes in the try main block could affect the try expression enter node as well as its previous nodes.
        tryMainBlockEnterNode.mergeIncomingFlow(shouldForkFlow = true)
    }

    fun exitTryMainBlock() {
        graphBuilder.exitTryMainBlock().mergeIncomingFlow()
    }

    fun enterCatchClause(catch: FirCatch) {
        // NB: fork to isolate effects inside the catch clause
        // Otherwise, changes in the catch clause could affect the previous node: try main block.
        graphBuilder.enterCatchClause(catch).mergeIncomingFlow(updateReceivers = true, shouldForkFlow = true)
    }

    fun exitCatchClause(catch: FirCatch) {
        graphBuilder.exitCatchClause(catch).mergeIncomingFlow()
    }

    fun enterFinallyBlock() {
        // NB: fork to isolate effects inside the finally block
        // Otherwise, changes in the finally block could affect the previous nodes: try main block and catch clauses.
        graphBuilder.enterFinallyBlock().mergeIncomingFlow(updateReceivers = true, shouldForkFlow = true)
    }

    fun exitFinallyBlock() {
        graphBuilder.exitFinallyBlock().mergeIncomingFlow()
    }

    fun exitTryExpression(callCompleted: Boolean) {
        val (tryExpressionExitNode, unionNode) = graphBuilder.exitTryExpression(callCompleted)
        // NB: fork to prevent effects after the try expression from being flown into the try expression
        // Otherwise, changes in any following nodes could affect the previous nodes, including try main block and finally block if any.
        tryExpressionExitNode.mergeIncomingFlow(shouldForkFlow = true)
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
                ?.makeConeTypeDefinitelyNotNullOrNotNull(components.session.typeContext)
                ?: return@let

            val variable = variableStorage.getOrCreateVariable(flow, receiver)
            if (variable.isReal()) {
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
        val flow = node.flow

        val variable = variableStorage.getOrCreateVariable(flow, safeCall)
        val receiverVariable = when (variable) {
            // There is some bug with invokes. See KT-36014
            is RealVariable -> variable.explicitReceiverVariable ?: return
            is SyntheticVariable -> variableStorage.getOrCreateVariable(flow, safeCall.receiver)
        }
        logicSystem.addImplication(flow, (variable notEq null) implies (receiverVariable notEq null))
        if (receiverVariable.isReal()) {
            logicSystem.addImplication(flow, (variable notEq null) implies (receiverVariable typeEq any))
        }
    }

    fun exitResolvedQualifierNode(resolvedQualifier: FirResolvedQualifier) {
        graphBuilder.exitResolvedQualifierNode(resolvedQualifier).mergeIncomingFlow()
    }

    fun enterCall() {
        graphBuilder.enterCall()
    }

    fun enterFunctionCall(functionCall: FirFunctionCall) {
        val lambdaArgs = functionCall.arguments.mapNotNull { (it as? FirAnonymousFunctionExpression)?.anonymousFunction }
        if (lambdaArgs.size > 1) {
            getOrCreateLocalVariableAssignmentAnalyzer(lambdaArgs.first())?.enterFunctionCallWithMultipleLambdaArgs(lambdaArgs)
        }
    }

    @OptIn(PrivateForInline::class)
    fun exitFunctionCall(functionCall: FirFunctionCall, callCompleted: Boolean) {
        val lambdaArgs = functionCall.arguments.mapNotNull { (it as? FirAnonymousFunctionExpression)?.anonymousFunction }
        if (lambdaArgs.size > 1) {
            getOrCreateLocalVariableAssignmentAnalyzer(lambdaArgs.first())?.enterFunctionCallWithMultipleLambdaArgs(lambdaArgs)
        }
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

    fun exitStringConcatenationCall(call: FirStringConcatenationCall) {
        val (callNode, unionNode) = graphBuilder.exitStringConcatenationCall(call)
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
        graphBuilder.exitConstExpression(constExpression).mergeIncomingFlow()
    }

    fun exitLocalVariableDeclaration(variable: FirProperty, hadExplicitType: Boolean) {
        val node = graphBuilder.exitVariableDeclaration(variable).mergeIncomingFlow()
        val initializer = variable.initializer ?: return
        exitVariableInitialization(node, initializer, variable, assignment = null, hadExplicitType)
    }

    fun exitVariableAssignment(assignment: FirVariableAssignment) {
        val node = graphBuilder.exitVariableAssignment(assignment).mergeIncomingFlow()
        val property = assignment.lValue.resolvedSymbol?.fir as? FirProperty ?: return
        // TODO: add unstable smartcast
        if (property.isLocal || !property.isVar) {
            exitVariableInitialization(node, assignment.rValue, property, assignment, hasExplicitType = false)
        }
        processConditionalContract(assignment)
    }

    private fun exitVariableInitialization(
        node: CFGNode<*>,
        initializer: FirExpression,
        property: FirProperty,
        assignment: FirVariableAssignment?,
        hasExplicitType: Boolean,
    ) {
        val flow = node.flow
        val propertyVariable = variableStorage.getOrCreateRealVariableWithoutUnwrappingAlias(
            flow,
            property.symbol,
            assignment ?: property,
            if (property.isVal) PropertyStability.STABLE_VALUE else PropertyStability.LOCAL_VAR
        )
        val isAssignment = assignment != null
        if (isAssignment) {
            logicSystem.removeLocalVariableAlias(flow, propertyVariable)
            logicSystem.removeAllAboutVariable(flow, propertyVariable)
            logicSystem.recordNewAssignment(flow, propertyVariable, context.newAssignmentIndex())
        }

        variableStorage.getOrCreateRealVariable(flow, initializer.symbol, initializer)
            ?.let { initializerVariable ->
                val isInitializerStable =
                    initializerVariable.isStable || (initializerVariable.hasLocalStability && initializer.isAccessToStableVariable())

                if (!hasExplicitType && isInitializerStable && (propertyVariable.hasLocalStability || propertyVariable.isStable)) {
                    logicSystem.addLocalVariableAlias(
                        flow, propertyVariable,
                        RealVariableAndType(initializerVariable, initializer.coneType)
                    )
                    // node.flow.addImplication((propertyVariable notEq null) implies (initializerVariable notEq null))
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
            flow.addTypeStatement(propertyVariable typeEq initializer.typeRef.coneType)
        }
    }

    private fun FirExpression.isAccessToStableVariable(): Boolean =
        this is FirQualifiedAccessExpression && !isAccessToUnstableLocalVariable(this)

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

    fun enterElvis(elvisExpression: FirElvisExpression) {
        graphBuilder.enterElvis(elvisExpression)
    }

    fun exitElvisLhs(elvisExpression: FirElvisExpression) {
        val (lhsExitNode, lhsIsNotNullNode, rhsEnterNode) = graphBuilder.exitElvisLhs(elvisExpression)
        lhsExitNode.mergeIncomingFlow()
        val flow = lhsExitNode.flow
        val lhsVariable = variableStorage.getOrCreateVariable(flow, elvisExpression.lhs)
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
        rhsEnterNode.flow = logicSystem.approveStatementsInsideFlow(
            flow,
            lhsVariable eq null,
            shouldForkFlow = true,
            shouldRemoveSynthetics = false
        ).also {
            logicSystem.updateAllReceivers(it)
        }
    }

    fun exitElvis(elvisExpression: FirElvisExpression) {
        val node = graphBuilder.exitElvis().mergeIncomingFlow()
        if (!components.session.languageVersionSettings.supportsFeature(LanguageFeature.BooleanElvisBoundSmartCasts)) return
        val lhs = elvisExpression.lhs
        val rhs = elvisExpression.rhs
        if (rhs is FirConstExpression<*> && rhs.kind == ConstantValueKind.Boolean) {
            if (lhs.typeRef.coneType.classId != StandardClassIds.Boolean) return

            val flow = node.flow
            // a ?: false == true -> a != null
            // a ?: true == false -> a != null
            val elvisVariable = variableStorage.getOrCreateVariable(flow, elvisExpression)
            val lhsVariable = variableStorage.getOrCreateVariable(flow, lhs)

            val value = rhs.value as Boolean
            flow.addImplication(elvisVariable.eq(!value) implies (lhsVariable.notEq(null)))
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

    private fun <T : CFGNode<*>> T.mergeIncomingFlow(
        // This flag should be set true if we're changing flow branches from one to another (e.g. in when, try->catch)
        updateReceivers: Boolean = false,
        shouldForkFlow: Boolean = false
    ): T = this.also { node ->
        val previousFlows = mutableListOf<FLOW>()
        var deadForwardCount = 0
        for (previousNode in previousNodes) {
            val incomingEdgeKind = node.incomingEdges.getValue(previousNode).kind
            if (node.isDead) {
                if (!incomingEdgeKind.isBack) {
                    previousFlows += previousNode.flow
                }
            } else if (incomingEdgeKind.usedInDfa) {
                previousFlows += previousNode.flow
            }
            if (incomingEdgeKind == EdgeKind.DeadForward) {
                deadForwardCount++
            }
        }
        var flow = logicSystem.joinFlow(previousFlows)
        // deadForwardCount should be added due to cases like merge after 'if (...) return else ...'
        if (updateReceivers || previousFlows.size + deadForwardCount > 1) {
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

    private fun FLOW.fork(): FLOW {
        return logicSystem.forkFlow(this)
    }

    private val CFGNode<*>.previousFlow: FLOW
        get() = firstPreviousNode.flow
}
