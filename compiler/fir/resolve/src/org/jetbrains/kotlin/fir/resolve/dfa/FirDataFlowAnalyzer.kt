/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.dfa

import org.jetbrains.kotlin.contracts.description.canBeRevisited
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.contracts.FirResolvedContractDescription
import org.jetbrains.kotlin.fir.contracts.description.ConeConditionalEffectDeclaration
import org.jetbrains.kotlin.fir.contracts.description.ConeReturnsEffectDeclaration
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.impl.FirDefaultPropertyAccessor
import org.jetbrains.kotlin.fir.declarations.utils.isLocal
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.impl.FirNoReceiverExpression
import org.jetbrains.kotlin.fir.references.FirControlFlowGraphReference
import org.jetbrains.kotlin.fir.resolve.*
import org.jetbrains.kotlin.fir.resolve.calls.ImplicitReceiverValue
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.*
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutorByMap
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.FirAbstractBodyResolveTransformer
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.resultType
import org.jetbrains.kotlin.fir.scopes.getFunctions
import org.jetbrains.kotlin.fir.scopes.impl.declaredMemberScope
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeParameterSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.types.ConstantValueKind
import org.jetbrains.kotlin.util.OperatorNameConventions
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull

class DataFlowAnalyzerContext<FLOW : Flow>(
    val graphBuilder: ControlFlowGraphBuilder,
    variableStorage: VariableStorageImpl,
    flowOnNodes: MutableMap<CFGNode<*>, FLOW>,
    val preliminaryLoopVisitor: PreliminaryLoopVisitor,
    val variablesClearedBeforeLoop: Stack<List<RealVariable>>,
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

        variableStorage = variableStorage.clear()
        flowOnNodes = mutableMapOf()

        preliminaryLoopVisitor.resetState()
        variablesClearedBeforeLoop.reset()
        firLocalVariableAssignmentAnalyzer = null
    }

    companion object {
        fun <FLOW : Flow> empty(session: FirSession): DataFlowAnalyzerContext<FLOW> =
            DataFlowAnalyzerContext(
                ControlFlowGraphBuilder(), VariableStorageImpl(session),
                mutableMapOf(), PreliminaryLoopVisitor(), stackOf()
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

                override fun receiverUpdated(symbol: FirBasedSymbol<*>, info: TypeStatement?) {
                    val index = receiverStack.getReceiverIndex(symbol) ?: return
                    val originalType = receiverStack.getOriginalType(index)
                    receiverStack.replaceReceiverType(index, info.smartCastedType(typeContext, originalType))
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
    protected abstract fun receiverUpdated(symbol: FirBasedSymbol<*>, info: TypeStatement?)

    private val graphBuilder get() = context.graphBuilder
    private val variableStorage get() = context.variableStorage

    private val any = components.session.builtinTypes.anyType.type
    private val nullableNothing = components.session.builtinTypes.nullableNothingType.type

    @PrivateForInline
    var ignoreFunctionCalls: Boolean = false

    // ----------------------------------- Requests -----------------------------------

    fun isAccessToUnstableLocalVariable(expression: FirExpression): Boolean {
        val analyzer = context.firLocalVariableAssignmentAnalyzer ?: return false
        val realFir = expression.unwrapElement() as? FirQualifiedAccessExpression ?: return false
        return analyzer.isAccessToUnstableLocalVariable(realFir)
    }

    open fun getTypeUsingSmartcastInfo(expression: FirExpression): Pair<PropertyStability, MutableList<ConeKotlinType>>? {
        val flow = graphBuilder.lastNode.flow
        val variable = variableStorage.getRealVariableWithoutUnwrappingAlias(flow, expression) ?: return null
        val types = flow.getTypeStatement(variable)?.exactType?.ifEmpty { null } ?: return null
        return variable.stability to types.toMutableList()
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
            resetReceivers()
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
        val invocationKind = anonymousFunction.invocationKind
        if (invocationKind == null || invocationKind.canBeRevisited()) {
            // TODO: if invocation can happen 0 times, there will be an edge from `functionEnterNode`
            //  to `functionExitNode`, so erasing statements here causes all information to be lost
            //  even though `statements from before && statements made inside the lambda` are correct.
            //     x = ""
            //     callUnknownNumberOfTimes { x = "" }
            //     /* x is String no matter how many times the lambda is called, but that information got lost */
            enterCapturingStatement(flowOnEntry, anonymousFunction)
        }
    }

    private fun exitAnonymousFunction(anonymousFunction: FirAnonymousFunction): FirControlFlowGraphReference {
        getOrCreateLocalVariableAssignmentAnalyzer(anonymousFunction)?.exitLocalFunction(
            anonymousFunction
        )
        val (functionExitNode, postponedLambdaExitNode, graph) = graphBuilder.exitAnonymousFunction(anonymousFunction)
        val invocationKind = anonymousFunction.invocationKind
        if (invocationKind == null || invocationKind.canBeRevisited()) {
            exitCapturingStatement(anonymousFunction)
        }
        functionExitNode.mergeIncomingFlow()
        if (postponedLambdaExitNode != null) {
            postponedLambdaExitNode.mergeIncomingFlow()
        } else {
            resetReceivers()
        }
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

    // ----------------------------------- Scripts ------------------------------------------

    fun enterScript(script: FirScript) {
        val res = graphBuilder.enterScript(script)
        res.mergeIncomingFlow()
    }

    fun exitScript(script: FirScript) {
        graphBuilder.exitScript(script)
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
        val operandVariable = variableStorage.getOrCreateIfReal(flow, typeOperatorCall.argument) ?: return
        when (val operation = typeOperatorCall.operation) {
            FirOperation.IS, FirOperation.NOT_IS -> {
                val isType = operation == FirOperation.IS
                when (type) {
                    // x is Nothing? <=> x == null
                    nullableNothing -> processEqNull(node, typeOperatorCall.argument, isType)
                    // x is Any <=> x != null
                    any -> processEqNull(node, typeOperatorCall.argument, !isType)
                    else -> {
                        val expressionVariable = variableStorage.createSynthetic(typeOperatorCall)
                        if (operandVariable.isReal()) {
                            flow.addImplication((expressionVariable eq isType) implies (operandVariable typeEq type))
                        }
                        if (!type.canBeNull) {
                            // x is (T & Any) => x != null
                            flow.addImplication((expressionVariable eq isType) implies (operandVariable notEq null))
                        } else if (type.isMarkedNullable) {
                            // x !is T? => x != null
                            flow.addImplication((expressionVariable eq !isType) implies (operandVariable notEq null))
                        } // else probably a type parameter, so which implication is correct depends on instantiation
                    }
                }
            }

            FirOperation.AS -> {
                if (operandVariable.isReal()) {
                    flow.addTypeStatement(operandVariable typeEq type)
                }
                if (!type.canBeNull) {
                    flow.commitOperationStatement(operandVariable notEq null)
                } else {
                    val expressionVariable = variableStorage.createSynthetic(typeOperatorCall)
                    flow.addImplication((expressionVariable notEq null) implies (operandVariable notEq null))
                    flow.addImplication((expressionVariable eq null) implies (operandVariable eq null))
                }
            }

            FirOperation.SAFE_AS -> {
                val expressionVariable = variableStorage.createSynthetic(typeOperatorCall)
                flow.addImplication((expressionVariable notEq null) implies (operandVariable notEq null))
                if (operandVariable.isReal()) {
                    flow.addImplication((expressionVariable notEq null) implies (operandVariable typeEq type))
                }
            }

            else -> throw IllegalStateException()
        }
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

    private fun processEqWithConst(
        node: EqualityOperatorCallNode, operand: FirExpression, const: FirConstExpression<*>, operation: FirOperation
    ) {
        val isEq = operation.isEq()
        if (const.kind == ConstantValueKind.Null) {
            return processEqNull(node, operand, isEq)
        }

        val flow = node.flow
        val operandVariable = variableStorage.getOrCreateIfReal(flow, operand) ?: return
        val expressionVariable = variableStorage.createSynthetic(node.fir)
        if (const.kind == ConstantValueKind.Boolean && operand.coneType.isBooleanOrNullableBoolean) {
            val expected = (const.value as Boolean)
            flow.addImplication((expressionVariable eq isEq) implies (operandVariable eq expected))
            if (operand.coneType.isBoolean) {
                flow.addImplication((expressionVariable eq !isEq) implies (operandVariable eq !expected))
            }
        } else {
            // expression == non-null const -> expression != null
            flow.addImplication((expressionVariable eq isEq) implies (operandVariable notEq null))
        }
    }

    private fun processEqNull(node: CFGNode<*>, operand: FirExpression, isEq: Boolean) {
        val flow = node.flow
        val operandVariable = variableStorage.getOrCreateIfReal(flow, operand) ?: return
        val expressionVariable = variableStorage.createSynthetic(node.fir)
        flow.addImplication((expressionVariable eq isEq) implies (operandVariable eq null))
        flow.addImplication((expressionVariable eq !isEq) implies (operandVariable notEq null))
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
        // TODO: should be `getOrCreateIfRealAndUnchanged(flow from LHS, flow, leftOperand)`, otherwise the statement will
        //  be added even if the value has changed in the RHS. Currently the only previous node is the RHS.
        val leftOperandVariable = variableStorage.getOrCreateIfReal(flow, leftOperand)
        val rightOperandVariable = variableStorage.getOrCreateIfReal(flow, rightOperand)
        if (leftOperandVariable == null && rightOperandVariable == null) return
        val expressionVariable = variableStorage.createSynthetic(node.fir)

        if (leftIsNullable || rightIsNullable) {
            // `a == b:Any` => `a != null`; the inverse is not true - we don't know when `a` *is* `null`
            val nullableOperand = if (leftIsNullable) leftOperandVariable else rightOperandVariable
            if (nullableOperand != null) {
                flow.addImplication((expressionVariable eq isEq) implies (nullableOperand notEq null))
            }
        }

        if (leftOperandVariable !is RealVariable && rightOperandVariable !is RealVariable) return

        if (operation == FirOperation.EQ || operation == FirOperation.NOT_EQ) {
            if (hasOverriddenEquals(leftOperandType)) return
        }

        if (leftOperandVariable is RealVariable) {
            flow.addImplication((expressionVariable eq isEq) implies (leftOperandVariable typeEq rightOperandType))
        }
        if (rightOperandVariable is RealVariable) {
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
        val flow = node.mergeIncomingFlow()
        val argumentVariable = variableStorage.getOrCreateIfReal(flow, checkNotNullCall.argument)
        if (argumentVariable != null) {
            flow.commitOperationStatement(argumentVariable notEq null)
        }
        unionNode?.unionFlowFromArguments()
    }

    // ----------------------------------- When -----------------------------------

    fun enterWhenExpression(whenExpression: FirWhenExpression) {
        graphBuilder.enterWhenExpression(whenExpression).mergeIncomingFlow()
    }

    fun enterWhenBranchCondition(whenBranch: FirWhenBranch) {
        graphBuilder.enterWhenBranchCondition(whenBranch).mergeWhenBranchEntryFlow()
    }

    private fun CFGNode<*>.mergeWhenBranchEntryFlow() {
        val previousConditionExitNode = previousNodes.singleOrNull()
        if (previousConditionExitNode is WhenBranchConditionExitNode) {
            val flow = mergeIncomingFlow()
            val previousCondition = previousConditionExitNode.fir.condition
            if (previousCondition.coneType.isBoolean) {
                val previousConditionVariable = variableStorage.get(flow, previousCondition) ?: return
                flow.commitOperationStatement(previousConditionVariable eq false)
            }
        } else { // first branch
            mergeIncomingFlow()
        }
    }

    fun exitWhenBranchCondition(whenBranch: FirWhenBranch) {
        val (conditionExitNode, resultEnterNode) = graphBuilder.exitWhenBranchCondition(whenBranch)
        val conditionExitFlow = conditionExitNode.mergeIncomingFlow()
        val resultEnterFlow = resultEnterNode.mergeIncomingFlow()
        // If the condition is invalid, don't generate smart casts to Any or Boolean.
        if (whenBranch.condition.coneType.isBoolean) {
            val conditionVariable = variableStorage.get(conditionExitFlow, whenBranch.condition) ?: return
            resultEnterFlow.commitOperationStatement(conditionVariable eq true)
        }
    }

    fun exitWhenBranchResult(whenBranch: FirWhenBranch) {
        graphBuilder.exitWhenBranchResult(whenBranch).mergeIncomingFlow()
    }

    fun exitWhenExpression(whenExpression: FirWhenExpression) {
        val (whenExitNode, syntheticElseNode, mergePostponedLambdaExitsNode) = graphBuilder.exitWhenExpression(whenExpression)
        syntheticElseNode?.mergeWhenBranchEntryFlow()
        whenExitNode.mergeIncomingFlow()
        mergePostponedLambdaExitsNode?.mergeIncomingFlow()
    }

    fun exitWhenSubjectExpression(expression: FirWhenSubjectExpression) {
        graphBuilder.exitWhenSubjectExpression(expression).mergeIncomingFlow()
    }

    // ----------------------------------- While Loop -----------------------------------

    fun enterWhileLoop(loop: FirLoop) {
        val (loopEnterNode, loopConditionEnterNode) = graphBuilder.enterWhileLoop(loop)
        loopEnterNode.mergeIncomingFlow()
        val loopConditionEnterFlow = loopConditionEnterNode.mergeIncomingFlow()
        enterCapturingStatement(loopConditionEnterFlow, loop)
    }

    fun exitWhileLoopCondition(loop: FirLoop) {
        val (loopConditionExitNode, loopBlockEnterNode) = graphBuilder.exitWhileLoopCondition(loop)
        val conditionExitFlow = loopConditionExitNode.mergeIncomingFlow()
        val blockEnterFlow = loopBlockEnterNode.mergeIncomingFlow()
        if (loop.condition.coneType.isBoolean) {
            val conditionVariable = variableStorage.get(conditionExitFlow, loop.condition) ?: return
            blockEnterFlow.commitOperationStatement(conditionVariable eq true)
        }
    }

    fun exitWhileLoop(loop: FirLoop) {
        val (conditionEnterNode, blockExitNode, exitNode) = graphBuilder.exitWhileLoop(loop)
        blockExitNode.mergeIncomingFlow()
        val possiblyChangedVariables = exitCapturingStatement(loop)
        // While analyzing the loop we might have added some backwards jumps to `conditionEnterNode` which weren't
        // there at the time its flow was computed - which is why we erased all information about `possiblyChangedVariables`
        // from it. Now that we have those edges, we can restore type information for the code after the loop.
        if (!possiblyChangedVariables.isNullOrEmpty()) {
            val conditionEnterFlow = conditionEnterNode.flow
            val loopEnterAndContinueFlows = conditionEnterNode.livePreviousFlows
            val conditionExitAndBreakFlows = exitNode.livePreviousFlows
            possiblyChangedVariables.forEach { variable ->
                // The statement about `variable` in `conditionEnterFlow` should be empty, so to obtain the new statement
                // we can simply add the now-known input to whatever was inferred from nothing so long as the value is the same.
                val statement = logicSystem.or(loopEnterAndContinueFlows.map { it.getTypeStatement(variable) ?: return@forEach })
                    ?: return@forEach
                for (beforeExitFlow in conditionExitAndBreakFlows) {
                    if (logicSystem.isSameValueIn(conditionEnterFlow, beforeExitFlow, variable)) {
                        beforeExitFlow.addTypeStatement(statement)
                    }
                }
            }
        }
        exitNode.mergeLoopExitFlow(exitNode.firstPreviousNode as LoopConditionExitNode)
    }

    private fun LoopExitNode.mergeLoopExitFlow(conditionExitNode: LoopConditionExitNode) {
        val flow = mergeIncomingFlow()
        if (conditionExitNode.isDead || previousNodes.count { !it.isDead } > 1) return
        if (conditionExitNode.fir.coneType.isBoolean) {
            val variable = variableStorage.get(flow, conditionExitNode.fir) ?: return
            flow.commitOperationStatement(variable eq false)
        }
    }

    private fun enterCapturingStatement(flow: FLOW, statement: FirStatement) {
        val reassignedNames = context.preliminaryLoopVisitor.enterCapturingStatement(statement)
        if (reassignedNames.isEmpty()) return
        // TODO: only choose the innermost variable for each name
        val possiblyChangedVariables = variableStorage.realVariables.values.filter {
            val identifier = it.identifier
            val symbol = identifier.symbol
            // Non-local vars can never produce stable smart casts anyway.
            identifier.dispatchReceiver == null && identifier.extensionReceiver == null &&
                    symbol is FirPropertySymbol && symbol.isVar && symbol.name in reassignedNames
        }
        for (variable in possiblyChangedVariables) {
            logicSystem.recordNewAssignment(flow, variable, context.newAssignmentIndex())
        }
        context.variablesClearedBeforeLoop.push(possiblyChangedVariables)
    }

    private fun exitCapturingStatement(statement: FirStatement): List<RealVariable>? {
        if (context.preliminaryLoopVisitor.exitCapturingStatement(statement).isEmpty()) return null
        return context.variablesClearedBeforeLoop.pop()
    }

    // ----------------------------------- Do while Loop -----------------------------------

    fun enterDoWhileLoop(loop: FirLoop) {
        val (loopEnterNode, loopBlockEnterNode) = graphBuilder.enterDoWhileLoop(loop)
        val loopEnterFlow = loopEnterNode.mergeIncomingFlow()
        enterCapturingStatement(loopEnterFlow, loop)
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
        loopExitNode.mergeLoopExitFlow(loopConditionExitNode)
        exitCapturingStatement(loop)
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
        graphBuilder.enterCatchClause(catch).mergeIncomingFlow()
    }

    fun exitCatchClause(catch: FirCatch) {
        graphBuilder.exitCatchClause(catch).mergeIncomingFlow()
    }

    fun enterFinallyBlock() {
        graphBuilder.enterFinallyBlock().mergeIncomingFlow()
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
        val flow = graphBuilder.exitQualifiedAccessExpression(qualifiedAccessExpression).mergeIncomingFlow()
        processConditionalContract(flow, qualifiedAccessExpression)
    }

    fun exitSmartCastExpression(smartCastExpression: FirSmartCastExpression) {
        graphBuilder.exitSmartCastExpression(smartCastExpression).mergeIncomingFlow()
    }

    fun enterSafeCallAfterNullCheck(safeCall: FirSafeCallExpression) {
        val flow = graphBuilder.enterSafeCall(safeCall).mergeIncomingFlow()
        val receiverVariable = variableStorage.getOrCreateIfReal(flow, safeCall.receiver) ?: return
        flow.commitOperationStatement(receiverVariable notEq null)
    }

    fun exitSafeCall(safeCall: FirSafeCallExpression) {
        val (node, mergePostponedLambdaExitsNode) = graphBuilder.exitSafeCall()
        val flow = node.mergeIncomingFlow()
        mergePostponedLambdaExitsNode?.mergeIncomingFlow()
        // If there is only 1 previous node, then this is LHS of `a?.b ?: c`; then the null-case
        // edge from `a` goes directly to `c` and this node's flow already assumes `b` executed.
        if (node.previousNodes.size < 2) return
        // Otherwise if the result is non-null, then `b` executed, which implies `a` is not null
        // and every statement from `b` holds.
        val expressionVariable = variableStorage.getOrCreate(flow, safeCall)
        // TODO? if the callee has non-null return type, then safe-call == null => receiver == null
        //   if (x?.toString() == null) { /* x == null */ }
        // TODO? all new implications in previous node's flow are valid here if receiver != null
        //  (that requires a second level of implications: receiver != null => condition => effect).
        flow.addAllConditionally(expressionVariable notEq null, node.lastPreviousNode.flow)
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
        val flow = functionCallNode.mergeIncomingFlow()
        processConditionalContract(flow, functionCall)
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
        flow = logicSystem.unionFlow(previousNodes.map { it.flow })
    }

    private fun FirQualifiedAccess.orderedArguments(callee: FirFunction): Array<out FirExpression?>? {
        val receiver = extensionReceiver.takeIf { it != FirNoReceiverExpression }
            ?: dispatchReceiver.takeIf { it != FirNoReceiverExpression }
        return when (this) {
            is FirFunctionCall -> {
                val argumentToParameter = resolvedArgumentMapping ?: return null
                val parameterToArgument = argumentToParameter.entries.associate { it.value to it.key.unwrapArgument() }
                Array(callee.valueParameters.size + 1) { i ->
                    if (i > 0) parameterToArgument[callee.valueParameters[i - 1]] else receiver
                }
            }
            is FirQualifiedAccessExpression -> arrayOf(receiver)
            is FirVariableAssignment -> arrayOf(receiver, rValue)
            else -> return null
        }
    }

    private fun processConditionalContract(flow: FLOW, qualifiedAccess: FirQualifiedAccess) {
        val callee = when (qualifiedAccess) {
            is FirFunctionCall -> qualifiedAccess.toResolvedCallableSymbol()?.fir as? FirSimpleFunction
            is FirQualifiedAccessExpression -> (qualifiedAccess.calleeReference.resolvedSymbol?.fir as? FirProperty)?.getter
            is FirVariableAssignment -> (qualifiedAccess.lValue.resolvedSymbol?.fir as? FirProperty)?.setter
            else -> null
        } ?: return

        if (callee.symbol.callableId == StandardClassIds.Callables.not) {
            // Special hardcoded contract for Boolean.not():
            //   returns(true) implies (this == false)
            //   returns(false) implies (this == true)
            return exitBooleanNot(flow, qualifiedAccess as FirFunctionCall)
        }

        val contractDescription = callee.contractDescription as? FirResolvedContractDescription ?: return
        val conditionalEffects = contractDescription.effects.mapNotNull { it.effect as? ConeConditionalEffectDeclaration }
        if (conditionalEffects.isEmpty()) return

        val arguments = qualifiedAccess.orderedArguments(callee) ?: return
        // TODO: should be `getOrCreateIfRealAndUnchanged(last flow of argument i, flow, it)`
        //                                                ^-- good luck finding that
        val argumentVariables = Array(arguments.size) { i -> arguments[i]?.let { variableStorage.getOrCreateIfReal(flow, it) } }
        if (argumentVariables.all { it == null }) return

        val typeParameters = callee.typeParameters
        val substitutor = if (typeParameters.isNotEmpty()) {
            @Suppress("UNCHECKED_CAST")
            val substitutionFromArguments = typeParameters.zip(qualifiedAccess.typeArguments).map { (typeParameterRef, typeArgument) ->
                typeParameterRef.symbol to typeArgument.toConeTypeProjection().type
            }.filter { it.second != null }.toMap() as Map<FirTypeParameterSymbol, ConeKotlinType>
            ConeSubstitutorByMap(substitutionFromArguments, components.session)
        } else {
            null
        }

        for (conditionalEffect in conditionalEffects) {
            val effect = conditionalEffect.effect as? ConeReturnsEffectDeclaration ?: continue
            val operation = effect.value.toOperation()
            val statements = logicSystem.approveContractStatement(
                flow, conditionalEffect.condition, argumentVariables, substitutor, removeApprovedOrImpossible = operation == null
            ) ?: continue // TODO: do what if the result is known to be false?
            if (operation == null) {
                flow.addAllStatements(statements)
            } else {
                val functionCallVariable = variableStorage.getOrCreate(flow, qualifiedAccess)
                flow.addAllConditionally(OperationStatement(functionCallVariable, operation), statements)
            }
        }
    }

    fun exitConstExpression(constExpression: FirConstExpression<*>) {
        if (constExpression.resultType is FirResolvedTypeRef) return
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
            val variable = variableStorage.getRealVariableWithoutUnwrappingAlias(flow, assignment)
            if (variable != null) {
                logicSystem.recordNewAssignment(flow, variable, context.newAssignmentIndex())
            }
        }
        processConditionalContract(flow, assignment)
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

        val initializerVariable = variableStorage.getOrCreateIfReal(flow, initializer)
        if (initializerVariable is RealVariable) {
            val isInitializerStable =
                initializerVariable.isStable || (initializerVariable.hasLocalStability && !isAccessToUnstableLocalVariable(initializer))
            if (!hasExplicitType && isInitializerStable && (propertyVariable.hasLocalStability || propertyVariable.isStable)) {
                // val a = ...
                // val b = a
                // if (b != null) { /* a != null */ }
                logicSystem.addLocalVariableAlias(flow, propertyVariable, initializerVariable)
            } else {
                // val a = ...
                // val b = a?.x
                // if (b != null) { /* a != null, but a.x could have changed */ }
                logicSystem.translateVariableFromConditionInStatements(flow, initializerVariable, propertyVariable)
            }
        } else if (initializerVariable != null) {
            // val b = x is String
            // if (b) { /* x is String */ }
            logicSystem.translateVariableFromConditionInStatements(flow, initializerVariable, propertyVariable)
        }

        if (isAssignment) {
            // `propertyVariable` can be an alias to `initializerVariable`, in which case this will add
            // a redundant type statement which is fine...probably. TODO: store initial type within the variable?
            flow.addTypeStatement(flow.unwrapVariable(propertyVariable) typeEq initializer.typeRef.coneType)
        }
    }

    private val RealVariable.isStable get() = stability == PropertyStability.STABLE_VALUE
    private val RealVariable.hasLocalStability get() = stability == PropertyStability.LOCAL_VAR


    fun exitThrowExceptionNode(throwExpression: FirThrowExpression) {
        graphBuilder.exitThrowExceptionNode(throwExpression).mergeIncomingFlow()
    }

    // ----------------------------------- Boolean operators -----------------------------------

    fun enterBinaryLogicExpression(binaryLogicExpression: FirBinaryLogicExpression) {
        graphBuilder.enterBinaryLogicExpression(binaryLogicExpression).mergeIncomingFlow()
    }

    fun exitLeftBinaryLogicExpressionArgument(binaryLogicExpression: FirBinaryLogicExpression) {
        val (leftExitNode, rightEnterNode) = graphBuilder.exitLeftBinaryLogicExpressionArgument(binaryLogicExpression)
        val leftExitFlow = leftExitNode.mergeIncomingFlow()
        val rightEnterFlow = rightEnterNode.mergeIncomingFlow()
        val leftOperandVariable = variableStorage.get(leftExitFlow, leftExitNode.firstPreviousNode.fir) ?: return
        val isAnd = binaryLogicExpression.kind == LogicOperationKind.AND
        rightEnterFlow.commitOperationStatement(leftOperandVariable eq isAnd)
    }

    fun exitBinaryLogicExpression(binaryLogicExpression: FirBinaryLogicExpression) {
        val node = graphBuilder.exitBinaryLogicExpression()
        val isAnd = binaryLogicExpression.kind == LogicOperationKind.AND
        val flowFromLeft = node.leftOperandNode.flow
        val flowFromRight = node.rightOperandNode.flow
        val flow = node.mergeIncomingFlow()

        val leftVariable = variableStorage.get(flowFromLeft, binaryLogicExpression.leftOperand)
        val leftIsBoolean = leftVariable != null && binaryLogicExpression.leftOperand.coneType.isBoolean
        if (!node.leftOperandNode.isDead && node.rightOperandNode.isDead) {
            // If the right operand does not terminate, then we know that the value of the entire expression
            // has to be saturating (true for or, false for and), and it has to be produced by the left operand.
            if (leftIsBoolean) {
                // Not checking for reassignments is safe since RHS did not execute.
                flow.commitOperationStatement(leftVariable!! eq !isAnd)
            }
        } else {
            val rightVariable = variableStorage.get(flowFromRight, binaryLogicExpression.rightOperand)
            val rightIsBoolean = rightVariable != null && binaryLogicExpression.rightOperand.coneType.isBoolean
            val operatorVariable = variableStorage.createSynthetic(binaryLogicExpression)
            // If `left && right` is true, then both are evaluated to true. If `left || right` is false, then both are false.
            // Approved type statements for RHS already contain everything implied by the corresponding value of LHS.
            val bothEvaluated = operatorVariable eq isAnd
            // TODO? `bothEvaluated` also implies all implications from RHS. This requires a second level
            //  of implications, which the logic system currently doesn't support. See also safe calls.
            flow.addAllConditionally(bothEvaluated, flowFromRight)
            if (rightIsBoolean) {
                flow.addAllConditionally(bothEvaluated, logicSystem.approveOperationStatement(flowFromRight, rightVariable!! eq isAnd))
            }
            // If `left && right` is false, then either `left` is false, or both were evaluated and `right` is false.
            // If `left || right` is true, then either `left` is true, or both were evaluated and `right` is true.
            if (leftIsBoolean && rightIsBoolean) {
                flow.addAllConditionally(
                    operatorVariable eq !isAnd,
                    logicSystem.orForTypeStatements(
                        // Not checking for reassignments is safe since we will only take statements that are also true in RHS
                        // (so they're true regardless of whether the variable ends up being reassigned or not).
                        logicSystem.approveOperationStatement(flowFromLeft, leftVariable!! eq !isAnd),
                        // TODO: and(approved from right, ...)? FE1.0 doesn't seem to handle that correctly either.
                        //   if (x is A || whatever(x as B)) { /* x is (A | B) */ }
                        logicSystem.approveOperationStatement(flowFromRight, rightVariable!! eq !isAnd),
                    )
                )
            }
        }
    }

    private fun exitBooleanNot(flow: FLOW, expression: FirFunctionCall) {
        val argumentVariable = variableStorage.get(flow, expression.dispatchReceiver) ?: return
        val expressionVariable = variableStorage.createSynthetic(expression)
        // Alternatively: (expression == true => argument == false) && (expression == false => argument == true)
        // Which implementation is faster and/or consumes less memory is an open question.
        logicSystem.translateVariableFromConditionInStatements(flow, argumentVariable, expressionVariable) {
            when (it.condition.operation) {
                Operation.EqTrue -> expressionVariable eq false implies it.effect
                Operation.EqFalse -> expressionVariable eq true implies it.effect
                // `argumentVariable eq/notEq null` shouldn't exist since `argumentVariable` is presumably `Boolean`
                else -> null
            }
        }
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
        graphBuilder.enterInitBlock(initBlock).mergeIncomingFlow()
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
        val lhsIsNotNullFlow = lhsIsNotNullNode.mergeIncomingFlow()
        val rhsEnterFlow = rhsEnterNode.mergeIncomingFlow()
        val lhsVariable = variableStorage.getOrCreateIfReal(flow, elvisExpression.lhs) ?: return
        lhsIsNotNullFlow.commitOperationStatement(lhsVariable notEq null)
        rhsEnterFlow.commitOperationStatement(lhsVariable eq null)
    }

    fun exitElvis(elvisExpression: FirElvisExpression, isLhsNotNull: Boolean) {
        val (node, mergePostponedLambdaExitsNode) = graphBuilder.exitElvis(isLhsNotNull)
        val flow = node.mergeIncomingFlow()
        mergePostponedLambdaExitsNode?.mergeIncomingFlow()
        // If LHS is never null, then the edge from RHS is dead and this node's flow already contains
        // all statements from LHS unconditionally.
        if (isLhsNotNull) return
        // For any predicate P(x), if P(v) != P(u ?: v) then u != null. In general this requires two levels of
        // implications, but for constant v the logic system can handle some basic cases of P(x).
        val rhs = (elvisExpression.rhs as? FirConstExpression<*>)?.value as? Boolean ?: return
        val elvisVariable = variableStorage.createSynthetic(elvisExpression)
        flow.addAllConditionally(elvisVariable eq !rhs, node.firstPreviousNode.flow)
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

    private val CFGNode<*>.livePreviousFlows: List<FLOW>
        get() = previousNodes.mapNotNull { it.takeIf { this.isDead || !it.isDead }?.flow }

    // Smart cast information is taken from `graphBuilder.lastNode`, but the problem with receivers specifically
    // is that they also affect tower resolver's scope stack. To allow accessing members on smart casted receivers,
    // we explicitly patch up the stack by calling `receiverUpdated` in a way that maintains consistency with
    // `getTypeUsingSmartcastInfo`; i.e. at any point between calls to this class' methods the types in the implicit
    // receiver stack also correspond to the data flow information attached to `graphBuilder.lastNode`.
    private var currentReceiverState: FLOW? = null

    // Generally when calling some method on `graphBuilder`, one of the nodes it returns is the new `lastNode`.
    // In that case `mergeIncomingFlow` will automatically ensure consistency once called on that node.
    private fun CFGNode<*>.mergeIncomingFlow(): FLOW {
        val previousFlows = previousNodes.mapNotNull {
            val incomingEdgeKind = incomingEdges.getValue(it).kind
            it.takeIf { incomingEdgeKind.usedInDfa || (isDead && incomingEdgeKind.usedInDeadDfa) }?.flow
        }
        val result = logicSystem.joinFlow(previousFlows).also { flow = it }
        if (graphBuilder.lastNodeOrNull == this) {
            // Here it is, the new `lastNode`. If the previous state is the only predecessor, then there is actually
            // nothing to update; `addTypeStatement` has already ensured we have the correct information.
            if (currentReceiverState == null || previousFlows.singleOrNull() != currentReceiverState) {
                updateAllReceivers(currentReceiverState, result)
            }
            currentReceiverState = result
        }
        return result
    }

    // In rare cases (like after exiting functions) after adding more nodes `graphBuilder` will revert the current
    // state to a previously created node, so none of the nodes it returned are `lastNode` and `mergeIncomingFlow`
    // will not ensure consistency. In that case an explicit call to `resetReceivers` is needed to roll back the stack
    // to that previously created node's state.
    private fun resetReceivers() {
        val currentFlow = graphBuilder.lastNodeOrNull?.flow
        updateAllReceivers(currentReceiverState, currentFlow)
        currentReceiverState = currentFlow
    }

    private fun updateAllReceivers(from: FLOW?, to: FLOW?) {
        receiverStack.forEach {
            variableStorage.getLocalVariable(it.boundSymbol)?.let { variable ->
                val newStatement = to?.getTypeStatement(variable)
                if (newStatement != from?.getTypeStatement(variable)) {
                    receiverUpdated(it.boundSymbol, newStatement)
                }
            }
        }
    }

    private fun FLOW.addImplication(statement: Implication) {
        logicSystem.addImplication(this, statement)
    }

    private fun FLOW.addTypeStatement(info: TypeStatement) {
        val newStatement = logicSystem.addTypeStatement(this, info) ?: return
        if (newStatement.variable.isThisReference && this === currentReceiverState) {
            receiverUpdated(newStatement.variable.identifier.symbol, newStatement)
        }
    }

    private fun FLOW.addAllStatements(statements: TypeStatements) {
        val newStatements = logicSystem.addTypeStatements(this, statements)
        if (this === currentReceiverState) {
            for (newStatement in newStatements) {
                if (newStatement.variable.isThisReference) {
                    receiverUpdated(newStatement.variable.identifier.symbol, newStatement)
                }
            }
        }
    }

    private fun FLOW.addAllConditionally(condition: OperationStatement, statements: TypeStatements) =
        statements.values.forEach { addImplication(condition implies it) }

    private fun FLOW.addAllConditionally(condition: OperationStatement, from: FLOW) =
        from.knownVariables.forEach {
            // Only add the statement if this variable is not aliasing another in `this` (but it could be aliasing in `from`).
            if (unwrapVariable(it) == it) addImplication(condition implies (from.getTypeStatement(it) ?: return@forEach))
        }

    private fun FLOW.commitOperationStatement(statement: OperationStatement) =
        addAllStatements(logicSystem.approveOperationStatement(this, statement, removeApprovedOrImpossible = true))

    private fun VariableStorageImpl.getOrCreateIfRealAndUnchanged(originalFlow: FLOW, currentFlow: FLOW, fir: FirElement) =
        getOrCreateIfReal(originalFlow, fir)?.takeIf { !it.isReal() || logicSystem.isSameValueIn(originalFlow, currentFlow, it) }
}
