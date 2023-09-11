/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.dfa

import kotlinx.collections.immutable.toPersistentSet
import org.jetbrains.kotlin.contracts.description.LogicOperationKind
import org.jetbrains.kotlin.contracts.description.canBeRevisited
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.contracts.description.ConeConditionalEffectDeclaration
import org.jetbrains.kotlin.fir.contracts.description.ConeReturnsEffectDeclaration
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.impl.FirDefaultPropertyAccessor
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.references.FirControlFlowGraphReference
import org.jetbrains.kotlin.fir.references.toResolvedPropertySymbol
import org.jetbrains.kotlin.fir.resolve.*
import org.jetbrains.kotlin.fir.resolve.calls.ImplicitReceiverValue
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.*
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutor
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutorByMap
import org.jetbrains.kotlin.fir.resolve.substitution.chain
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.FirAbstractBodyResolveTransformer
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.resultType
import org.jetbrains.kotlin.fir.resolve.transformers.unwrapAnonymousFunctionExpression
import org.jetbrains.kotlin.fir.scopes.getFunctions
import org.jetbrains.kotlin.fir.scopes.impl.toConeType
import org.jetbrains.kotlin.fir.scopes.unsubstitutedScope
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeParameterSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.types.ConstantValueKind
import org.jetbrains.kotlin.util.OperatorNameConventions

class DataFlowAnalyzerContext(session: FirSession) {
    val graphBuilder = ControlFlowGraphBuilder()
    val preliminaryLoopVisitor = PreliminaryLoopVisitor()
    val variablesClearedBeforeLoop = stackOf<List<RealVariable>>()
    internal val variableAssignmentAnalyzer = FirLocalVariableAssignmentAnalyzer()

    var variableStorage = VariableStorageImpl(session)
        private set

    private var assignmentCounter = 0

    fun newAssignmentIndex(): Int {
        return assignmentCounter++
    }

    fun reset() {
        graphBuilder.reset()
        preliminaryLoopVisitor.resetState()
        variablesClearedBeforeLoop.reset()
        variableAssignmentAnalyzer.reset()
        variableStorage = variableStorage.clear()
    }
}

@OptIn(DfaInternals::class)
abstract class FirDataFlowAnalyzer(
    protected val components: FirAbstractBodyResolveTransformer.BodyResolveTransformerComponents,
    private val context: DataFlowAnalyzerContext
) {
    companion object {
        fun createFirDataFlowAnalyzer(
            components: FirAbstractBodyResolveTransformer.BodyResolveTransformerComponents,
            dataFlowAnalyzerContext: DataFlowAnalyzerContext
        ): FirDataFlowAnalyzer =
            object : FirDataFlowAnalyzer(components, dataFlowAnalyzerContext) {
                override val receiverStack: PersistentImplicitReceiverStack
                    get() = components.implicitReceiverStack as PersistentImplicitReceiverStack

                private val visibilityChecker = components.session.visibilityChecker
                private val typeContext = components.session.typeContext

                override fun receiverUpdated(symbol: FirBasedSymbol<*>, info: TypeStatement?) {
                    val index = receiverStack.getReceiverIndex(symbol) ?: return
                    val originalType = receiverStack.getOriginalType(index)
                    receiverStack.replaceReceiverType(index, info.smartCastedType(typeContext, originalType))
                }

                override val logicSystem: LogicSystem =
                    object : LogicSystem(components.session.typeContext) {
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

    protected abstract val logicSystem: LogicSystem
    protected abstract val receiverStack: Iterable<ImplicitReceiverValue<*>>
    protected abstract fun receiverUpdated(symbol: FirBasedSymbol<*>, info: TypeStatement?)

    private val graphBuilder get() = context.graphBuilder
    private val variableStorage get() = context.variableStorage

    private val any = components.session.builtinTypes.anyType.type
    private val nullableNothing = components.session.builtinTypes.nullableNothingType.type

    // ----------------------------------- Requests -----------------------------------

    fun isAccessToUnstableLocalVariable(expression: FirExpression): Boolean =
        context.variableAssignmentAnalyzer.isAccessToUnstableLocalVariable(expression)

    open fun getTypeUsingSmartcastInfo(expression: FirExpression): Pair<PropertyStability, MutableList<ConeKotlinType>>? {
        val flow = graphBuilder.lastNode.flow
        val variable = variableStorage.getRealVariableWithoutUnwrappingAlias(flow, expression) ?: return null
        val types = flow.getTypeStatement(variable)?.exactType?.ifEmpty { null } ?: return null
        return variable.stability to types.toMutableList()
    }

    fun returnExpressionsOfAnonymousFunctionOrNull(function: FirAnonymousFunction): Collection<FirAnonymousFunctionReturnExpressionInfo>? =
        graphBuilder.returnExpressionsOfAnonymousFunction(function)

    fun returnExpressionsOfAnonymousFunction(function: FirAnonymousFunction): Collection<FirAnonymousFunctionReturnExpressionInfo> =
        returnExpressionsOfAnonymousFunctionOrNull(function)
            ?: error("anonymous function ${function.render()} not analyzed")

    // ----------------------------------- Named function -----------------------------------

    fun enterFunction(function: FirFunction) {
        if (function is FirDefaultPropertyAccessor) return

        val (localFunctionNode, functionEnterNode) = if (function is FirAnonymousFunction) {
            null to graphBuilder.enterAnonymousFunction(function)
        } else {
            graphBuilder.enterFunction(function)
        }
        localFunctionNode?.mergeIncomingFlow()
        functionEnterNode.mergeIncomingFlow { _, flow ->
            if (function is FirAnonymousFunction && function.invocationKind?.canBeRevisited() == true) {
                enterRepeatableStatement(flow, function)
            }
        }
        context.variableAssignmentAnalyzer.enterFunction(function)
    }

    fun exitFunction(function: FirFunction): FirControlFlowGraphReference? {
        if (function is FirDefaultPropertyAccessor) return null

        context.variableAssignmentAnalyzer.exitFunction()
        if (function is FirAnonymousFunction && function.invocationKind?.canBeRevisited() == true) {
            exitRepeatableStatement(function)
        }

        if (function is FirAnonymousFunction) {
            val (functionExitNode, postponedLambdaExitNode, graph) = graphBuilder.exitAnonymousFunction(function)
            functionExitNode.mergeIncomingFlow()
            postponedLambdaExitNode?.mergeIncomingFlow()
            resetReceivers() // roll back to state before function
            return FirControlFlowGraphReferenceImpl(graph)
        }

        val (node, graph) = graphBuilder.exitFunction(function)
        node.mergeIncomingFlow()
        if (!graphBuilder.isTopLevel) {
            for (valueParameter in function.valueParameters) {
                variableStorage.removeRealVariable(valueParameter.symbol)
            }
        }
        val info = DataFlowInfo(variableStorage)
        resetReceivers()
        return FirControlFlowGraphReferenceImpl(graph, info)
    }

    // ----------------------------------- Anonymous function -----------------------------------

    fun enterAnonymousFunctionExpression(anonymousFunctionExpression: FirAnonymousFunctionExpression) {
        graphBuilder.enterAnonymousFunctionExpression(anonymousFunctionExpression)?.mergeIncomingFlow()
    }

    // ----------------------------------- Files ------------------------------------------

    fun enterFile(file: FirFile, buildGraph: Boolean) {
        graphBuilder.enterFile(file, buildGraph)?.mergeIncomingFlow()
    }

    fun exitFile(): ControlFlowGraph? {
        val (node, graph) = graphBuilder.exitFile()
        if (node != null) {
            node.mergeIncomingFlow()
        } else {
            resetReceivers()
        }
        return graph
    }

    // ----------------------------------- Classes -----------------------------------

    fun enterClass(klass: FirClass, buildGraph: Boolean) {
        val (outerNode, enterNode) = graphBuilder.enterClass(klass, buildGraph)
        outerNode?.mergeIncomingFlow()
        enterNode?.mergeIncomingFlow()
        context.variableAssignmentAnalyzer.enterClass(klass)
    }

    fun exitClass(): ControlFlowGraph? {
        context.variableAssignmentAnalyzer.exitClass()
        val (node, graph) = graphBuilder.exitClass()
        if (node != null) {
            node.mergeIncomingFlow()
        } else {
            resetReceivers() // to state before class initialization
        }
        return graph
    }

    fun exitAnonymousObjectExpression(anonymousObjectExpression: FirAnonymousObjectExpression) {
        graphBuilder.exitAnonymousObjectExpression(anonymousObjectExpression)?.mergeIncomingFlow()
    }

    // ----------------------------------- Scripts ------------------------------------------

    fun enterScript(script: FirScript) {
        graphBuilder.enterScript(script).mergeIncomingFlow()
    }

    fun exitScript(): ControlFlowGraph {
        val (node, graph) = graphBuilder.exitScript()
        node.mergeIncomingFlow()
        return graph
    }

    // ----------------------------------- Code Fragment ------------------------------------------

    fun enterCodeFragment(codeFragment: FirCodeFragment) {
        graphBuilder.enterCodeFragment(codeFragment).mergeIncomingFlow { _, flow ->
            val smartCasts = codeFragment.codeFragmentContext?.smartCasts.orEmpty()
            for ((originalRealVariable, exactTypes) in smartCasts) {
                val realVariable = variableStorage.getOrPut(originalRealVariable.identifier) { originalRealVariable }
                val typeStatement = PersistentTypeStatement(realVariable, exactTypes.toPersistentSet())
                flow.addTypeStatement(typeStatement)
            }
        }
    }

    fun exitCodeFragment(): ControlFlowGraph {
        val (node, graph) = graphBuilder.exitCodeFragment()
        node.mergeIncomingFlow()
        return graph
    }
    // ----------------------------------- Value parameters (and it's defaults) -----------------------------------

    fun enterValueParameter(valueParameter: FirValueParameter) {
        val (outerNode, innerNode) = graphBuilder.enterValueParameter(valueParameter) ?: return
        outerNode.mergeIncomingFlow()
        innerNode.mergeIncomingFlow()
    }

    fun exitValueParameter(valueParameter: FirValueParameter): ControlFlowGraph? {
        val (innerNode, outerNode, graph) = graphBuilder.exitValueParameter(valueParameter) ?: return null
        innerNode.mergeIncomingFlow()
        outerNode.mergeIncomingFlow()
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

    fun exitDelegateExpression(fir: FirExpression) {
        graphBuilder.exitDelegateExpression(fir).mergeIncomingFlow()
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
        graphBuilder.exitTypeOperatorCall(typeOperatorCall).mergeIncomingFlow { _, flow ->
            if (typeOperatorCall.operation !in FirOperation.TYPES) return@mergeIncomingFlow
            addTypeOperatorStatements(flow, typeOperatorCall)
        }
    }

    private fun addTypeOperatorStatements(flow: MutableFlow, typeOperatorCall: FirTypeOperatorCall) {
        val type = typeOperatorCall.conversionTypeRef.coneType
        val operandVariable = variableStorage.getOrCreateIfReal(flow, typeOperatorCall.argument) ?: return
        when (val operation = typeOperatorCall.operation) {
            FirOperation.IS, FirOperation.NOT_IS -> {
                val isType = operation == FirOperation.IS
                when (type) {
                    // x is Nothing? <=> x == null
                    nullableNothing -> processEqNull(flow, typeOperatorCall, typeOperatorCall.argument, isType)
                    // x is Any <=> x != null
                    any -> processEqNull(flow, typeOperatorCall, typeOperatorCall.argument, !isType)
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
        val leftIsNull = leftIsNullConst || leftOperand.resolvedType.isNullableNothing && !rightIsNullConst
        val rightIsNull = rightIsNullConst || rightOperand.resolvedType.isNullableNothing && !leftIsNullConst

        node.mergeIncomingFlow { _, flow ->
            when {
                leftConst != null && rightConst != null -> return@mergeIncomingFlow
                leftIsNull -> processEqNull(flow, equalityOperatorCall, rightOperand, operation.isEq())
                rightIsNull -> processEqNull(flow, equalityOperatorCall, leftOperand, operation.isEq())
                leftConst != null -> processEqConst(flow, equalityOperatorCall, rightOperand, leftConst, operation.isEq())
                rightConst != null -> processEqConst(flow, equalityOperatorCall, leftOperand, rightConst, operation.isEq())
                else -> processEq(flow, equalityOperatorCall, leftOperand, rightOperand, operation)
            }
        }
    }

    private fun processEqConst(
        flow: MutableFlow,
        expression: FirExpression,
        operand: FirExpression,
        const: FirConstExpression<*>,
        isEq: Boolean
    ) {
        if (const.kind == ConstantValueKind.Null) {
            return processEqNull(flow, expression, operand, isEq)
        }

        val operandVariable = variableStorage.getOrCreateIfReal(flow, operand) ?: return
        val expressionVariable = variableStorage.createSynthetic(expression)
        if (const.kind == ConstantValueKind.Boolean && operand.resolvedType.isBooleanOrNullableBoolean) {
            val expected = (const.value as Boolean)
            flow.addImplication((expressionVariable eq isEq) implies (operandVariable eq expected))
            if (operand.resolvedType.isBoolean) {
                flow.addImplication((expressionVariable eq !isEq) implies (operandVariable eq !expected))
            }
        } else {
            // expression == non-null const -> expression != null
            flow.addImplication((expressionVariable eq isEq) implies (operandVariable notEq null))
        }
    }

    private fun processEqNull(flow: MutableFlow, expression: FirExpression, operand: FirExpression, isEq: Boolean) {
        val operandVariable = variableStorage.getOrCreateIfReal(flow, operand) ?: return
        val expressionVariable = variableStorage.createSynthetic(expression)
        flow.addImplication((expressionVariable eq isEq) implies (operandVariable eq null))
        flow.addImplication((expressionVariable eq !isEq) implies (operandVariable notEq null))
    }

    private fun processEq(
        flow: MutableFlow,
        expression: FirExpression,
        leftOperand: FirExpression,
        rightOperand: FirExpression,
        operation: FirOperation,
    ) {
        val isEq = operation.isEq()
        val leftOperandType = leftOperand.resolvedType
        val rightOperandType = rightOperand.resolvedType
        val leftIsNullable = leftOperandType.isMarkedNullable
        val rightIsNullable = rightOperandType.isMarkedNullable

        if (leftIsNullable && rightIsNullable) {
            // The logic system is not complex enough to express a second level of implications this creates:
            // if either `== null` then this creates the same implications as a constant null comparison,
            // otherwise the same as if the corresponding `...IsNullable` is false.
            return
        }

        // Ideally it should be `getOrCreateIfRealAndUnchanged(flow from LHS, flow, leftOperand)`, otherwise the statement will
        //  be added even if the value has changed in the RHS. Currently, the only previous node is the RHS.
        // But seems like everything works and with current implementation
        val leftOperandVariable = variableStorage.getOrCreateIfReal(flow, leftOperand)
        val rightOperandVariable = variableStorage.getOrCreateIfReal(flow, rightOperand)
        if (leftOperandVariable == null && rightOperandVariable == null) return
        val expressionVariable = variableStorage.createSynthetic(expression)

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

        return this.unsubstitutedScope(
            session, components.scopeSession, withForcedTypeCalculator = false, memberRequiredPhase = FirResolvePhase.STATUS
        ).getFunctions(OperatorNameConventions.EQUALS).any {
            !it.isSubstitutionOrIntersectionOverride && it.fir.isEquals(session) &&
                    it.dispatchReceiverClassLookupTagOrNull() == this.toLookupTag()
        }
    }

    // ----------------------------------- Jump -----------------------------------

    fun enterJump(jump: FirJump<*>) {
        graphBuilder.enterJump(jump)
    }

    fun exitJump(jump: FirJump<*>) {
        graphBuilder.exitJump(jump).mergeIncomingFlow()
    }

    // ----------------------------------- Check not null call -----------------------------------

    fun enterCheckNotNullCall() {
        graphBuilder.enterCall()
    }

    fun exitCheckNotNullCall(checkNotNullCall: FirCheckNotNullCall, callCompleted: Boolean) {
        graphBuilder.exitCheckNotNullCall(checkNotNullCall, callCompleted).mergeIncomingFlow { _, flow ->
            val argumentVariable = variableStorage.getOrCreateIfReal(flow, checkNotNullCall.argument) ?: return@mergeIncomingFlow
            flow.commitOperationStatement(argumentVariable notEq null)
        }
    }

    // ----------------------------------- When -----------------------------------

    fun enterWhenExpression(whenExpression: FirWhenExpression) {
        graphBuilder.enterWhenExpression(whenExpression).mergeIncomingFlow()
    }

    fun enterWhenBranchCondition(whenBranch: FirWhenBranch) {
        graphBuilder.enterWhenBranchCondition(whenBranch).mergeWhenBranchEntryFlow()
    }

    private fun CFGNode<*>.mergeWhenBranchEntryFlow() = mergeIncomingFlow { _, flow ->
        val previousConditionExitNode = previousNodes.singleOrNull() as? WhenBranchConditionExitNode ?: return@mergeIncomingFlow
        val previousCondition = previousConditionExitNode.fir.condition
        if (!previousCondition.resolvedType.isBoolean) return@mergeIncomingFlow
        val previousConditionVariable = variableStorage.get(flow, previousCondition) ?: return@mergeIncomingFlow
        flow.commitOperationStatement(previousConditionVariable eq false)
    }

    fun exitWhenBranchCondition(whenBranch: FirWhenBranch) {
        val (conditionExitNode, resultEnterNode) = graphBuilder.exitWhenBranchCondition(whenBranch)
        conditionExitNode.mergeIncomingFlow()
        resultEnterNode.mergeIncomingFlow { _, flow ->
            // If the condition is invalid, don't generate smart casts to Any or Boolean.
            if (whenBranch.condition.resolvedType.isBoolean) {
                val conditionVariable = variableStorage.get(flow, whenBranch.condition) ?: return@mergeIncomingFlow
                flow.commitOperationStatement(conditionVariable eq true)
            }
        }
    }

    fun exitWhenBranchResult(whenBranch: FirWhenBranch) {
        graphBuilder.exitWhenBranchResult(whenBranch).mergeIncomingFlow()
    }

    fun exitWhenExpression(whenExpression: FirWhenExpression, callCompleted: Boolean) {
        val (whenExitNode, syntheticElseNode) = graphBuilder.exitWhenExpression(whenExpression, callCompleted)
        syntheticElseNode?.mergeWhenBranchEntryFlow()
        whenExitNode.mergeIncomingFlow()
    }

    fun exitWhenSubjectExpression(expression: FirWhenSubjectExpression) {
        graphBuilder.exitWhenSubjectExpression(expression).mergeIncomingFlow()
    }

    // ----------------------------------- While Loop -----------------------------------

    fun enterWhileLoop(loop: FirLoop) {
        val (loopEnterNode, loopConditionEnterNode) = graphBuilder.enterWhileLoop(loop)
        loopEnterNode.mergeIncomingFlow()
        loopConditionEnterNode.mergeIncomingFlow { _, flow -> enterRepeatableStatement(flow, loop) }
    }

    fun exitWhileLoopCondition(loop: FirLoop) {
        val (loopConditionExitNode, loopBlockEnterNode) = graphBuilder.exitWhileLoopCondition(loop)
        loopConditionExitNode.mergeIncomingFlow()
        loopBlockEnterNode.mergeIncomingFlow { _, flow ->
            if (loop.condition.resolvedType.isBoolean) {
                val conditionVariable = variableStorage.get(flow, loop.condition) ?: return@mergeIncomingFlow
                flow.commitOperationStatement(conditionVariable eq true)
            }
        }
    }

    fun exitWhileLoop(loop: FirLoop) {
        val (conditionEnterNode, blockExitNode, exitNode) = graphBuilder.exitWhileLoop(loop)
        blockExitNode.mergeIncomingFlow()
        exitNode.mergeIncomingFlow { path, flow ->
            processWhileLoopExit(path, flow, exitNode, conditionEnterNode)
            processLoopExit(flow, exitNode, exitNode.firstPreviousNode as LoopConditionExitNode)
        }
    }

    private fun processWhileLoopExit(path: FlowPath, flow: MutableFlow, node: LoopExitNode, conditionEnterNode: LoopConditionEnterNode) {
        val possiblyChangedVariables = exitRepeatableStatement(node.fir)
        if (possiblyChangedVariables.isNullOrEmpty()) return
        // While analyzing the loop we might have added some backwards jumps to `conditionEnterNode` which weren't
        // there at the time its flow was computed - which is why we erased all information about `possiblyChangedVariables`
        // from it. Now that we have those edges, we can restore type information for the code after the loop.
        val conditionEnterFlow = conditionEnterNode.getFlow(path)
        val loopEnterAndContinueFlows = conditionEnterNode.previousLiveNodes.map { it.getFlow(path) }
        val conditionExitAndBreakFlows = node.previousLiveNodes.map { it.getFlow(path) }
        possiblyChangedVariables.forEach { variable ->
            // The statement about `variable` in `conditionEnterFlow` should be empty, so to obtain the new statement
            // we can simply add the now-known input to whatever was inferred from nothing so long as the value is the same.
            val toAdd = logicSystem.or(loopEnterAndContinueFlows.map { it.getTypeStatement(variable) ?: return@forEach })
                ?.takeIf { it.isNotEmpty } ?: return@forEach
            val newStatement = logicSystem.or(conditionExitAndBreakFlows.map {
                val atExit = it.getTypeStatement(variable)
                if (logicSystem.isSameValueIn(conditionEnterFlow, it, variable)) {
                    logicSystem.and(atExit, toAdd)
                } else {
                    atExit ?: return@forEach
                }
            }) ?: return@forEach
            flow.addTypeStatement(newStatement)
        }
    }

    private fun processLoopExit(flow: MutableFlow, node: LoopExitNode, conditionExitNode: LoopConditionExitNode) {
        if (conditionExitNode.isDead || node.previousNodes.count { !it.isDead } > 1) return
        if (conditionExitNode.fir.resolvedType.isBoolean) {
            val variable = variableStorage.get(flow, conditionExitNode.fir) ?: return
            flow.commitOperationStatement(variable eq false)
        }
    }

    private fun enterRepeatableStatement(flow: MutableFlow, statement: FirStatement) {
        val reassignedNames = context.preliminaryLoopVisitor.enterCapturingStatement(statement)
        if (reassignedNames.isEmpty()) return
        // TODO: only choose the innermost variable for each name, KT-59688
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

    private fun exitRepeatableStatement(statement: FirStatement): List<RealVariable>? {
        if (context.preliminaryLoopVisitor.exitCapturingStatement(statement).isEmpty()) return null
        return context.variablesClearedBeforeLoop.pop()
    }

    // ----------------------------------- Do while Loop -----------------------------------

    fun enterDoWhileLoop(loop: FirLoop) {
        val (loopEnterNode, loopBlockEnterNode) = graphBuilder.enterDoWhileLoop(loop)
        loopEnterNode.mergeIncomingFlow { _, flow -> enterRepeatableStatement(flow, loop) }
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
        loopExitNode.mergeIncomingFlow { _, flow ->
            processLoopExit(flow, loopExitNode, loopConditionExitNode)
        }
        exitRepeatableStatement(loop)
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
        val node = graphBuilder.enterFinallyBlock()
        node.mergeIncomingFlow()
        node.createAlternateFlows()
    }

    fun exitFinallyBlock() {
        graphBuilder.exitFinallyBlock().mergeIncomingFlow()
    }

    fun exitTryExpression(callCompleted: Boolean) {
        graphBuilder.exitTryExpression(callCompleted).mergeIncomingFlow()
    }

    // ----------------------------------- Resolvable call -----------------------------------

    fun exitQualifiedAccessExpression(qualifiedAccessExpression: FirQualifiedAccessExpression) {
        graphBuilder.exitQualifiedAccessExpression(qualifiedAccessExpression).mergeIncomingFlow { _, flow ->
            processConditionalContract(flow, qualifiedAccessExpression)
        }
    }

    fun exitSmartCastExpression(smartCastExpression: FirSmartCastExpression) {
        graphBuilder.exitSmartCastExpression(smartCastExpression).mergeIncomingFlow()
    }

    fun enterSafeCallAfterNullCheck(safeCall: FirSafeCallExpression) {
        graphBuilder.enterSafeCall(safeCall).mergeIncomingFlow { _, flow ->
            val receiverVariable = variableStorage.getOrCreateIfReal(flow, safeCall.receiver) ?: return@mergeIncomingFlow
            flow.commitOperationStatement(receiverVariable notEq null)
        }
    }

    fun exitSafeCall(safeCall: FirSafeCallExpression) {
        val node = graphBuilder.exitSafeCall()
        node.mergeIncomingFlow { path, flow ->
            // If there is only 1 previous node, then this is LHS of `a?.b ?: c`; then the null-case
            // edge from `a` goes directly to `c` and this node's flow already assumes `b` executed.
            if (node.previousNodes.size < 2) return@mergeIncomingFlow
            // Otherwise if the result is non-null, then `b` executed, which implies `a` is not null
            // and every statement from `b` holds.
            val expressionVariable = variableStorage.getOrCreate(flow, safeCall)
            // TODO? all new implications in previous node's flow are valid here if receiver != null
            //  (that requires a second level of implications: receiver != null => condition => effect).
            //  KT-59689
            flow.addAllConditionally(expressionVariable notEq null, node.lastPreviousNode.getFlow(path))
        }
    }

    fun exitResolvedQualifierNode(resolvedQualifier: FirResolvedQualifier) {
        graphBuilder.exitResolvedQualifierNode(resolvedQualifier).mergeIncomingFlow()
    }

    fun enterCallArguments(call: FirStatement, arguments: List<FirExpression>) {
        val lambdas = arguments.mapNotNull { it.unwrapAnonymousFunctionExpression() }
        context.variableAssignmentAnalyzer.enterFunctionCall(lambdas)
        graphBuilder.enterCall()
        graphBuilder.enterCallArguments(call, lambdas)
    }

    fun exitCallArguments() {
        graphBuilder.exitCallArguments()?.mergeIncomingFlow()
    }

    fun exitFunctionCall(functionCall: FirFunctionCall, callCompleted: Boolean) {
        context.variableAssignmentAnalyzer.exitFunctionCall(callCompleted)
        graphBuilder.exitFunctionCall(functionCall, callCompleted).mergeIncomingFlow { _, flow ->
            processConditionalContract(flow, functionCall)
        }
    }

    fun exitDelegatedConstructorCall(call: FirDelegatedConstructorCall, callCompleted: Boolean) {
        context.variableAssignmentAnalyzer.exitFunctionCall(callCompleted)
        graphBuilder.exitDelegatedConstructorCall(call, callCompleted).mergeIncomingFlow()
    }

    fun enterStringConcatenationCall() {
        graphBuilder.enterCall()
    }

    fun exitStringConcatenationCall(call: FirStringConcatenationCall) {
        graphBuilder.exitStringConcatenationCall(call).mergeIncomingFlow()
    }

    private fun FirStatement.orderedArguments(callee: FirFunction): Array<out FirExpression?>? {
        fun FirQualifiedAccessExpression.firstReceiver(): FirExpression? {
            return extensionReceiver ?: dispatchReceiver
        }

        val receiver = when (this) {
            is FirQualifiedAccessExpression -> firstReceiver()
            is FirVariableAssignment -> (lValue as? FirQualifiedAccessExpression)?.firstReceiver()
            else -> null
        }

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

    private fun processConditionalContract(flow: MutableFlow, qualifiedAccess: FirStatement) {
        // contracts has no effect on non-body resolve stages
        if (!components.transformer.baseTransformerPhase.isBodyResolve) return

        // TODO: Consider using something besides `toResolvedCallableSymbol` as the latter only works
        //  for completed calls without candidates (KT-61055 for tracking)
        val callee = when (qualifiedAccess) {
            is FirFunctionCall -> qualifiedAccess.toResolvedCallableSymbol()?.fir as? FirSimpleFunction
            is FirQualifiedAccessExpression -> qualifiedAccess.calleeReference.toResolvedPropertySymbol()?.fir?.getter
            is FirVariableAssignment -> qualifiedAccess.calleeReference?.toResolvedPropertySymbol()?.fir?.setter
            else -> null
        } ?: return

        if (callee.symbol.callableId == StandardClassIds.Callables.not) {
            // Special hardcoded contract for Boolean.not():
            //   returns(true) implies (this == false)
            //   returns(false) implies (this == true)
            return exitBooleanNot(flow, qualifiedAccess as FirFunctionCall)
        }

        val originalFunction = callee.originalIfFakeOverride()
        val contractDescription = (originalFunction?.symbol ?: callee.symbol).resolvedContractDescription ?: return
        val conditionalEffects = contractDescription.effects.mapNotNull { it.effect as? ConeConditionalEffectDeclaration }
        if (conditionalEffects.isEmpty()) return

        val arguments = qualifiedAccess.orderedArguments(callee) ?: return
        val argumentVariables = Array(arguments.size) { i -> arguments[i]?.let { variableStorage.getOrCreateIfReal(flow, it) } }
        if (argumentVariables.all { it == null }) return

        val typeParameters = callee.typeParameters
        val typeArgumentsSubstitutor = if (typeParameters.isNotEmpty() && qualifiedAccess is FirQualifiedAccessExpression) {
            @Suppress("UNCHECKED_CAST")
            val substitutionFromArguments = typeParameters.zip(qualifiedAccess.typeArguments).map { (typeParameterRef, typeArgument) ->
                typeParameterRef.symbol to typeArgument.toConeTypeProjection().type
            }.filter { it.second != null }.toMap() as Map<FirTypeParameterSymbol, ConeKotlinType>
            ConeSubstitutorByMap(substitutionFromArguments, components.session)
        } else {
            ConeSubstitutor.Empty
        }


        val substitutor = if (originalFunction == null) {
            typeArgumentsSubstitutor
        } else {
            val map = originalFunction.symbol.typeParameterSymbols.zip(typeParameters.map { it.symbol.toConeType() }).toMap()
            ConeSubstitutorByMap(map, components.session).chain(typeArgumentsSubstitutor)
        }

        for (conditionalEffect in conditionalEffects) {
            val effect = conditionalEffect.effect as? ConeReturnsEffectDeclaration ?: continue
            val operation = effect.value.toOperation()
            val statements = logicSystem.approveContractStatement(conditionalEffect.condition, argumentVariables, substitutor) {
                logicSystem.approveOperationStatement(flow, it, removeApprovedOrImpossible = operation == null)
            } ?: continue // TODO: do what if the result is known to be false?
            if (operation == null) {
                flow.addAllStatements(statements)
            } else {
                val functionCallVariable = variableStorage.getOrCreate(flow, qualifiedAccess)
                flow.addAllConditionally(OperationStatement(functionCallVariable, operation), statements)
            }
        }
    }

    fun exitConstExpression(constExpression: FirConstExpression<*>) {
        if (constExpression.isResolved) return
        graphBuilder.exitConstExpression(constExpression).mergeIncomingFlow()
    }

    fun exitLocalVariableDeclaration(variable: FirProperty, hadExplicitType: Boolean) {
        graphBuilder.exitVariableDeclaration(variable).mergeIncomingFlow { _, flow ->
            val initializer = variable.initializer ?: return@mergeIncomingFlow
            exitVariableInitialization(flow, initializer, variable, assignmentLhs = null, hadExplicitType)
        }
    }

    fun exitVariableAssignment(assignment: FirVariableAssignment) {
        graphBuilder.exitVariableAssignment(assignment).mergeIncomingFlow { _, flow ->
            val property = assignment.calleeReference?.toResolvedPropertySymbol()?.fir ?: return@mergeIncomingFlow
            if (property.isLocal || property.isVal) {
                exitVariableInitialization(flow, assignment.rValue, property, assignment.lValue, hasExplicitType = false)
            } else {
                val variable = variableStorage.getRealVariableWithoutUnwrappingAlias(flow, assignment)
                if (variable != null) {
                    logicSystem.recordNewAssignment(flow, variable, context.newAssignmentIndex())
                }
            }
            processConditionalContract(flow, assignment)
        }
    }

    private fun exitVariableInitialization(
        flow: MutableFlow,
        initializer: FirExpression,
        property: FirProperty,
        assignmentLhs: FirExpression?,
        hasExplicitType: Boolean,
    ) {
        val propertyVariable = variableStorage.getOrCreateRealVariableWithoutUnwrappingAliasForPropertyInitialization(
            flow, property.symbol, assignmentLhs ?: property
        )
        val isAssignment = assignmentLhs != null
        if (isAssignment) {
            logicSystem.recordNewAssignment(flow, propertyVariable, context.newAssignmentIndex())
        }

        val initializerVariable = variableStorage.getOrCreateIfReal(flow, initializer)
        if (initializerVariable is RealVariable) {
            val isInitializerStable = initializerVariable.isStableOrLocalStableAccess(initializer)
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
        } else if (initializerVariable != null && propertyVariable.isStable) {
            // val b = x is String
            // if (b) { /* x is String */ }
            logicSystem.translateVariableFromConditionInStatements(flow, initializerVariable, propertyVariable)
        }

        if (isAssignment) {
            // `propertyVariable` can be an alias to `initializerVariable`, in which case this will add
            // a redundant type statement which is fine...probably
            flow.addTypeStatement(flow.unwrapVariable(propertyVariable) typeEq initializer.resolvedType)
        }
    }

    private val RealVariable.isStable get() = stability == PropertyStability.STABLE_VALUE
    private val RealVariable.hasLocalStability get() = stability == PropertyStability.LOCAL_VAR

    private fun RealVariable.isStableOrLocalStableAccess(access: FirExpression): Boolean {
        return isStable || (hasLocalStability && !isAccessToUnstableLocalVariable(access))
    }

    fun exitThrowExceptionNode(throwExpression: FirThrowExpression) {
        graphBuilder.exitThrowExceptionNode(throwExpression).mergeIncomingFlow()
    }

    // ----------------------------------- Boolean operators -----------------------------------

    fun enterBinaryLogicExpression(binaryLogicExpression: FirBinaryLogicExpression) {
        graphBuilder.enterBinaryLogicExpression(binaryLogicExpression).mergeIncomingFlow()
    }

    fun exitLeftBinaryLogicExpressionArgument(binaryLogicExpression: FirBinaryLogicExpression) {
        val (leftExitNode, rightEnterNode) = graphBuilder.exitLeftBinaryLogicExpressionArgument(binaryLogicExpression)
        leftExitNode.mergeIncomingFlow()
        rightEnterNode.mergeIncomingFlow { _, flow ->
            val leftOperandVariable = variableStorage.get(flow, binaryLogicExpression.leftOperand) ?: return@mergeIncomingFlow
            val isAnd = binaryLogicExpression.kind == LogicOperationKind.AND
            flow.commitOperationStatement(leftOperandVariable eq isAnd)
        }
    }

    fun exitBinaryLogicExpression(binaryLogicExpression: FirBinaryLogicExpression) {
        graphBuilder.exitBinaryLogicExpression(binaryLogicExpression).mergeBinaryLogicOperatorFlow()
    }

    private fun AbstractBinaryExitNode<FirBinaryLogicExpression>.mergeBinaryLogicOperatorFlow() = mergeIncomingFlow { path, flow ->
        val isAnd = fir.kind == LogicOperationKind.AND
        val flowFromLeft = leftOperandNode.getFlow(path)
        val flowFromRight = rightOperandNode.getFlow(path)

        val leftVariable = variableStorage.get(flowFromLeft, fir.leftOperand)
        val leftIsBoolean = leftVariable != null && fir.leftOperand.resolvedType.isBoolean
        if (!leftOperandNode.isDead && rightOperandNode.isDead) {
            // If the right operand does not terminate, then we know that the value of the entire expression
            // has to be saturating (true for or, false for and), and it has to be produced by the left operand.
            if (leftIsBoolean) {
                // Not checking for reassignments is safe since RHS did not execute.
                flow.commitOperationStatement(leftVariable!! eq !isAnd)
            }
        } else {
            val rightVariable = variableStorage.get(flowFromRight, fir.rightOperand)
            val rightIsBoolean = rightVariable != null && fir.rightOperand.resolvedType.isBoolean
            val operatorVariable = variableStorage.createSynthetic(fir)
            // If `left && right` is true, then both are evaluated to true. If `left || right` is false, then both are false.
            // Approved type statements for RHS already contain everything implied by the corresponding value of LHS.
            val bothEvaluated = operatorVariable eq isAnd
            // TODO? `bothEvaluated` also implies all implications from RHS. This requires a second level
            //  of implications, which the logic system currently doesn't support. See also safe calls. KT-59689
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
                        // TODO: and(approved from right, ...)? FE1.0 doesn't seem to handle that correctly either. KT-59690
                        //   if (x is A || whatever(x as B)) { /* x is (A | B) */ }
                        logicSystem.approveOperationStatement(flowFromRight, rightVariable!! eq !isAnd),
                    )
                )
            }
        }
    }

    private fun exitBooleanNot(flow: MutableFlow, expression: FirFunctionCall) {
        val argumentVariable = variableStorage.get(flow, expression.dispatchReceiver!!) ?: return
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

    fun enterAnnotation() {
        graphBuilder.enterFakeExpression().mergeIncomingFlow()
    }

    fun exitAnnotation() {
        graphBuilder.exitFakeExpression()
    }

    // ----------------------------------- Init block -----------------------------------

    fun enterInitBlock(initBlock: FirAnonymousInitializer) {
        graphBuilder.enterInitBlock(initBlock).mergeIncomingFlow()
    }

    fun exitInitBlock(): ControlFlowGraph {
        val (node, controlFlowGraph) = graphBuilder.exitInitBlock()
        node.mergeIncomingFlow()
        return controlFlowGraph
    }

    // ----------------------------------- Contract description -----------------------------------

    fun enterContractDescription() {
        graphBuilder.enterFakeExpression().mergeIncomingFlow()
    }

    fun exitContractDescription() {
        graphBuilder.exitFakeExpression()
    }

    // ----------------------------------- Elvis -----------------------------------

    fun enterElvis(elvisExpression: FirElvisExpression) {
        graphBuilder.enterElvis(elvisExpression)
    }

    fun exitElvisLhs(elvisExpression: FirElvisExpression) {
        val (lhsExitNode, lhsIsNotNullNode, rhsEnterNode) = graphBuilder.exitElvisLhs(elvisExpression)
        lhsExitNode.mergeIncomingFlow()

        fun getLhsVariable(path: FlowPath): DataFlowVariable? =
            variableStorage.getOrCreateIfReal(lhsExitNode.getFlow(path), elvisExpression.lhs)

        lhsIsNotNullNode.mergeIncomingFlow { path, flow ->
            getLhsVariable(path)?.let { flow.commitOperationStatement(it notEq null) }
        }
        rhsEnterNode.mergeIncomingFlow { path, flow ->
            getLhsVariable(path)?.let { flow.commitOperationStatement(it eq null) }
        }
    }

    fun exitElvis(elvisExpression: FirElvisExpression, isLhsNotNull: Boolean, callCompleted: Boolean) {
        val node = graphBuilder.exitElvis(isLhsNotNull, callCompleted)
        node.mergeIncomingFlow { path, flow ->
            // If LHS is never null, then the edge from RHS is dead and this node's flow already contains
            // all statements from LHS unconditionally.
            if (isLhsNotNull) return@mergeIncomingFlow

            val elvisVariable by lazy { variableStorage.createSynthetic(elvisExpression) }

            // If (x ?: null) != null then x != null
            @OptIn(UnresolvedExpressionTypeAccess::class) // Lambdas can have unresolved type here, see KT-61837
            if (elvisExpression.rhs.coneTypeOrNull?.isNullableNothing == true) {
                val lhsVariable = variableStorage.getOrCreateIfReal(flow, elvisExpression.lhs)
                if (lhsVariable != null) {
                    flow.addImplication((elvisVariable notEq null) implies (lhsVariable notEq null))
                }
            }

            // If (null ?: x) != null then x != null
            @OptIn(UnresolvedExpressionTypeAccess::class) // Lambdas can have unresolved type here, see KT-61837
            if (elvisExpression.lhs.coneTypeOrNull?.isNullableNothing == true) {
                val rhsVariable = variableStorage.getOrCreateIfReal(flow, elvisExpression.rhs)
                if (rhsVariable != null) {
                    flow.addImplication((elvisVariable notEq null) implies (rhsVariable notEq null))
                }
            }

            // For any predicate P(x), if P(v) != P(u ?: v) then u != null. In general this requires two levels of
            // implications, but for constant v the logic system can handle some basic cases of P(x).
            val rhs = (elvisExpression.rhs as? FirConstExpression<*>)?.value as? Boolean
            if (rhs != null) {
                flow.addAllConditionally(elvisVariable eq !rhs, node.firstPreviousNode.getFlow(path))
            }
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

    // Smart cast information is taken from `graphBuilder.lastNode`, but the problem with receivers specifically
    // is that they also affect tower resolver's scope stack. To allow accessing members on smart casted receivers,
    // we explicitly patch up the stack by calling `receiverUpdated` in a way that maintains consistency with
    // `getTypeUsingSmartcastInfo`; i.e. at any point between calls to this class' methods the types in the implicit
    // receiver stack also correspond to the data flow information attached to `graphBuilder.lastNode`.
    private var currentReceiverState: Flow? = null

    private fun CFGNode<*>.buildDefaultFlow(
        builder: (FlowPath, MutableFlow) -> Unit,
    ): MutableFlow {
        val previousFlows = previousNodes.mapNotNull { node ->
            val edge = edgeFrom(node)
            if (!usedInDfa(edge)) return@mapNotNull null

            // `MergePostponedLambdaExitsNode` nodes form a parallel data flow graph. We never compute
            // data flow for any of them until reaching a completed call.
            if (node is MergePostponedLambdaExitsNode && !node.flowInitialized) node.mergeIncomingFlow()

            // For CFGNodes that are the end of alternate flows, use the alternate flow associated with the edge label.
            if (node is FinallyBlockExitNode) {
                val alternatePath = FlowPath.CfgEdge(edge.label, node.fir)
                node.getAlternateFlow(alternatePath) ?: node.flow
            } else {
                node.flow
            }
        }
        val result = logicSystem.joinFlow(previousFlows, isUnion)
        if (graphBuilder.lastNodeOrNull == this) {
            // Here it is, the new `lastNode`. If the previous state is the only predecessor, then there is actually
            // nothing to update; `addTypeStatement` has already ensured we have the correct information.
            if (currentReceiverState == null || previousFlows.singleOrNull() != currentReceiverState) {
                updateAllReceivers(currentReceiverState, result)
            }
            currentReceiverState = result
        }
        return result.also { builder(FlowPath.Default, it) }
    }

    private fun CFGNode<*>.buildAlternateFlow(
        path: FlowPath.CfgEdge,
        builder: (FlowPath, MutableFlow) -> Unit,
    ): MutableFlow {
        val alternateFlowStart = this is FinallyBlockEnterNode
        val previousFlows = previousNodes.mapNotNull { node ->
            val edge = edgeFrom(node)
            if (!usedInDfa(edge)) return@mapNotNull null

            // For CFGNodes that cause alternate flow paths to be created, only edges with matching labels should be merged. However, when
            // an alternate flow is being propagated through one of these CFGNodes - i.e., when the FirElements do not match - only
            // NormalPath edges should be merged.
            if (alternateFlowStart) {
                if (path.fir == this.fir && edge.label != path.label) {
                    return@mapNotNull null
                } else if (path.fir != this.fir && edge.label != NormalPath) {
                    return@mapNotNull null
                }
            }

            node.getAlternateFlow(path) ?: node.flow
        }
        val result = logicSystem.joinFlow(previousFlows, isUnion)
        return result.also { builder(path, it) }
    }

    // Generally when calling some method on `graphBuilder`, one of the nodes it returns is the new `lastNode`.
    // In that case `mergeIncomingFlow` will automatically ensure consistency once called on that node.
    @OptIn(CfgInternals::class)
    private fun CFGNode<*>.mergeIncomingFlow(
        builder: (FlowPath, MutableFlow) -> Unit = { _, _ -> },
    ) {
        // Always build the default flow path for all nodes.
        val mutableDefaultFlow = buildDefaultFlow(builder)
        val defaultFlow = mutableDefaultFlow.freeze().also { this.flow = it }
        if (currentReceiverState === mutableDefaultFlow) {
            currentReceiverState = defaultFlow
        }

        // Propagate alternate flows from previous nodes.
        propagateAlternateFlows(builder)
    }

    @OptIn(CfgInternals::class)
    private fun CFGNode<*>.propagateAlternateFlows(
        builder: (FlowPath, MutableFlow) -> Unit,
    ) {
        val propagatedPaths = mutableSetOf<FlowPath>()
        for (node in previousNodes) {
            if (node.alternateFlowPaths.isEmpty()) continue

            val edge = edgeFrom(node)
            // Only propagate alternate flows which originate along a normal path edge and are used in DFA.
            if (edge.label != NormalPath || !usedInDfa(edge)) continue

            for (path in node.alternateFlowPaths) {
                // If the source node is the end of alternate flows, do not propagate the alternate flows which have ended.
                if (path !is FlowPath.CfgEdge || !graphBuilder.withinFinallyBlock(path.fir)) continue

                if (propagatedPaths.add(path)) {
                    addAlternateFlow(path, buildAlternateFlow(path, builder).freeze())
                }
            }
        }
    }

    @OptIn(CfgInternals::class)
    private fun CFGNode<*>.createAlternateFlows(
        builder: (FlowPath, MutableFlow) -> Unit = { _, _ -> },
    ) {
        val createdLabels = mutableSetOf<EdgeLabel>()
        for (node in previousNodes) {
            val edge = edgeFrom(node)
            if (edge.label == UncaughtExceptionPath || !usedInDfa(edge)) continue

            if (createdLabels.add(edge.label)) {
                val path = FlowPath.CfgEdge(edge.label, this.fir)
                addAlternateFlow(path, buildAlternateFlow(path, builder).freeze())
            }
        }
    }

    private fun CFGNode<*>.getFlow(path: FlowPath): PersistentFlow {
        return when (path) {
            FlowPath.Default -> flow
            else -> getAlternateFlow(path) ?: error("no alternate flow for $path")
        }
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

    private fun updateAllReceivers(from: Flow?, to: Flow?) {
        receiverStack.forEach {
            variableStorage.getLocalVariable(it.boundSymbol)?.let { variable ->
                val newStatement = to?.getTypeStatement(variable)
                if (newStatement != from?.getTypeStatement(variable)) {
                    receiverUpdated(it.boundSymbol, newStatement)
                }
            }
        }
    }

    private fun MutableFlow.addImplication(statement: Implication) {
        logicSystem.addImplication(this, statement)
    }

    private fun MutableFlow.addTypeStatement(info: TypeStatement) {
        val newStatement = logicSystem.addTypeStatement(this, info) ?: return
        if (newStatement.variable.isThisReference && this === currentReceiverState) {
            receiverUpdated(newStatement.variable.identifier.symbol, newStatement)
        }
    }

    private fun MutableFlow.addAllStatements(statements: TypeStatements) =
        statements.values.forEach { addTypeStatement(it) }

    private fun MutableFlow.addAllConditionally(condition: OperationStatement, statements: TypeStatements) =
        statements.values.forEach { addImplication(condition implies it) }

    private fun MutableFlow.addAllConditionally(condition: OperationStatement, from: Flow) =
        from.knownVariables.forEach {
            // Only add the statement if this variable is not aliasing another in `this` (but it could be aliasing in `from`).
            if (unwrapVariable(it) == it) addImplication(condition implies (from.getTypeStatement(it) ?: return@forEach))
        }

    private fun MutableFlow.commitOperationStatement(statement: OperationStatement) =
        addAllStatements(logicSystem.approveOperationStatement(this, statement, removeApprovedOrImpossible = true))
}
