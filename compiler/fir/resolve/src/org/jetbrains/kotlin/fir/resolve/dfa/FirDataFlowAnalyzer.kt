/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.dfa

import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toPersistentSet
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.contracts.description.LogicOperationKind
import org.jetbrains.kotlin.contracts.description.canBeRevisited
import org.jetbrains.kotlin.descriptors.isObject
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.contracts.description.*
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.impl.FirDefaultPropertyAccessor
import org.jetbrains.kotlin.fir.declarations.utils.isReplSnippetDeclaration
import org.jetbrains.kotlin.fir.declarations.utils.lambdaArgumentParent
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.references.*
import org.jetbrains.kotlin.fir.resolve.*
import org.jetbrains.kotlin.fir.resolve.calls.ImplicitValue
import org.jetbrains.kotlin.fir.resolve.calls.candidate.candidate
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.*
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutor
import org.jetbrains.kotlin.fir.resolve.substitution.chain
import org.jetbrains.kotlin.fir.resolve.substitution.substitutorByMap
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.FirAbstractBodyResolveTransformer
import org.jetbrains.kotlin.fir.resolve.transformers.unwrapAtoms
import org.jetbrains.kotlin.fir.scopes.impl.toConeType
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirEnumEntrySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeParameterSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.types.ConstantValueKind
import org.jetbrains.kotlin.types.SmartcastStability

class DataFlowAnalyzerContext private constructor(
    private val session: FirSession,
    graphBuilder: ControlFlowGraphBuilder,
    variableAssignmentAnalyzer: FirLocalVariableAssignmentAnalyzer,
    variableStorage: VariableStorage,
    private var assignmentCounter: Int
) {
    constructor(session: FirSession) : this(
        session,
        graphBuilder = ControlFlowGraphBuilder(),
        variableAssignmentAnalyzer = FirLocalVariableAssignmentAnalyzer(),
        variableStorage = VariableStorage(session),
        assignmentCounter = 0
    )

    /**
     * Builds a deep independent copy of this [DataFlowAnalyzerContext].
     * The copy is not affected by changes in this context.
     *
     * @param [firMapper] A mapper to be applied on all [FirElement]s and [FirBasedSymbol]s referenced in the snapshot.
     */
    @OptIn(CfgInternals::class)
    fun createSnapshot(firMapper: SnapshotFirMapper): DataFlowAnalyzerContextSnapshot {
        val copier = ControlFlowGraphCopier()

        val snapshotContext = DataFlowAnalyzerContext(
            session,
            graphBuilder = graphBuilder.createSnapshot(copier),
            variableAssignmentAnalyzer = variableAssignmentAnalyzer.createSnapshot(firMapper),
            variableStorage = variableStorage.createSnapshot(),
            assignmentCounter = assignmentCounter
        )

        copier.finish()

        return DataFlowAnalyzerContextSnapshot(snapshotContext, copier.graphMapping)
    }

    /**
     * Replaces all state of this [DataFlowAnalyzerContext] with those from [source].
     *
     * The method does not perform any deep copying, so the [source] context will be affected by changes in this one.
     * If you need to avoid this, call [createSnapshot] first.
     */
    fun resetFrom(source: DataFlowAnalyzerContext) {
        reset()

        graphBuilder = source.graphBuilder
        variableAssignmentAnalyzer = source.variableAssignmentAnalyzer
        variableStorage = source.variableStorage
        assignmentCounter = source.assignmentCounter
    }

    /**
     * Clears all intermediate state of this [DataFlowAnalyzerContext].
     * Are calling [reset], the context is identical to the newly created one.
     */
    fun reset() {
        graphBuilder.reset()
        variableAssignmentAnalyzer.reset()
        variableStorage = VariableStorage(session)
    }

    @CfgInternals
    val currentGraph: ControlFlowGraph
        get() = graphBuilder.currentGraph

    internal var graphBuilder: ControlFlowGraphBuilder = graphBuilder
        private set

    internal var variableAssignmentAnalyzer: FirLocalVariableAssignmentAnalyzer = variableAssignmentAnalyzer
        private set

    internal var variableStorage: VariableStorage = variableStorage
        private set

    fun newAssignmentIndex(): Int {
        return assignmentCounter++
    }
}

/**
 * A mapper for elements and symbols referenced in the control flow graph snapshot.
 *
 * Once created, the snapshot may be applied to a different tree of [FirElement]s.
 * The mapper allows patching references to corresponding elements in the new tree.
 */
@CfgInternals
interface SnapshotFirMapper {
    fun <T : FirBasedSymbol<*>> mapSymbol(symbol: T): T
    fun <T : FirElement> mapElement(element: T): T
}

/**
 * A deep independent snapshot of the DataFlowAnalyzerContext.
 *
 * @param context The snapshot itself.
 * @param graphMapping A mapping between graphs in the original context and in the snapshot.
 */
class DataFlowAnalyzerContextSnapshot(
    val context: DataFlowAnalyzerContext,
    val graphMapping: Map<ControlFlowGraph, ControlFlowGraph>
)

@OptIn(DfaInternals::class)
abstract class FirDataFlowAnalyzer(
    protected val components: FirAbstractBodyResolveTransformer.BodyResolveTransformerComponents,
    private val context: DataFlowAnalyzerContext,
) : SessionHolder {
    companion object {
        fun createFirDataFlowAnalyzer(
            components: FirAbstractBodyResolveTransformer.BodyResolveTransformerComponents,
            dataFlowAnalyzerContext: DataFlowAnalyzerContext,
        ): FirDataFlowAnalyzer =
            object : FirDataFlowAnalyzer(components, dataFlowAnalyzerContext) {
                override val receiverStack: ImplicitValueStorage
                    get() = components.implicitValueStorage

                private val visibilityChecker = components.session.visibilityChecker
                private val typeContext = components.session.typeContext

                @OptIn(ImplicitValue.ImplicitValueInternals::class)
                override fun implicitUpdated(info: TypeStatement) {
                    val dataFlowVariable = info.variable as? RealVariable ?: error("Not a real variable: ${info.variable}")
                    receiverStack.replaceImplicitValueType(dataFlowVariable.symbol, info.smartCastedType(typeContext))
                }

                override val logicSystem: LogicSystem =
                    object : LogicSystem(components.session.typeContext) {
                        override val variableStorage: VariableStorage
                            get() = dataFlowAnalyzerContext.variableStorage

                        override fun ConeKotlinType.isAcceptableForSmartcast(): Boolean {
                            if (this.isNullableNothing) return false
                            return when (this) {
                                is ConeClassLikeType -> {
                                    val symbol =
                                        fullyExpandedType().lookupTag.toSymbol() ?: return false
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

    override val session: FirSession
        get() = components.session

    protected abstract val logicSystem: LogicSystem
    protected abstract val receiverStack: ImplicitValueStorage
    protected abstract fun implicitUpdated(info: TypeStatement)

    private val graphBuilder get() = context.graphBuilder
    private val variableStorage get() = context.variableStorage

    private val any = components.session.builtinTypes.anyType.coneType
    private val nullableNothing = components.session.builtinTypes.nullableNothingType.coneType

    // ----------------------------------- Requests -----------------------------------

    /**
     * `var`s are normally stable because flows track assignments, but they can be captured by blocks that will be evaluated
     * later (namely local functions, lambdas without "callsInPlace" contracts, and classes). In that case they become unstable
     * if there are any assignments that could execute while these blocks are accessible, which is tracked by
     * [FirLocalVariableAssignmentAnalyzer].
     *
     *    var x = ...
     *    /* x is stable here */
     *    if (p) {
     *      val lambda = { /* x is unstable here - assignment below could execute before the lambda is called */ }
     *      x = ...
     *    } else if (p2) {
     *      val lambda = { /* x is stable here - the assignments above and below cannot affect this lambda */ }
     *    } else {
     *      x = ...
     *    }
     *
     * When [types] are provided, these assignments are additionally filtered by whether they invalidate this type information:
     * if the assigned value is known to be a subtype of all provided types, then it actually doesn't matter if the assignment
     * executed or not -- the smartcast is correct either way, despite the instability.
     *
     * When [types] are **not** provided, **any** assignments cause the variable to be considered unstable.
     */
    private fun RealVariable.isUnstableLocalVar(types: Set<ConeKotlinType>?): Boolean =
        context.variableAssignmentAnalyzer.isUnstableInCurrentScope(symbol.fir, types, components.session) ||
                dispatchReceiver?.isUnstableLocalVar(types = null) == true

    private fun DataFlowVariable.getStability(flow: Flow, targetTypes: Set<ConeKotlinType>?): SmartcastStability =
        if (this is RealVariable) {
            getStability(flow, components.session).let {
                if (it == SmartcastStability.CAPTURED_VARIABLE && !isUnstableLocalVar(targetTypes))
                    SmartcastStability.STABLE_VALUE
                else it
            }
        } else SmartcastStability.STABLE_VALUE

    /**
     * Retrieve smartcast type information [FirDataFlowAnalyzer] may have for the specified variable access expression. Type information
     * is **stateful** and changes as the FIR tree is navigated by [FirDataFlowAnalyzer].
     *
     * @param expression The variable access expression.
     */
    inline fun getTypeUsingSmartcastInfo(
        expression: FirExpression,
        augmentTypeStatement: (DataFlowVariable, TypeStatement?) -> TypeStatement? = { _, statement -> statement },
    ): SmartCastStatement? {
        val flow = currentSmartCastPosition ?: return null
        var variable: DataFlowVariable = SyntheticVariable(expression)
        val typeStatement = augmentTypeStatement(variable, extractTypeStatementFrom(flow, variable))?.takeIf { it.isNotEmpty }
            ?: run {
                // Can have an unstable alias to a stable variable, so don't resolve aliases here.
                variable = flow.getVariableWithoutUnwrappingAlias(expression, createReal = false) ?: return null
                augmentTypeStatement(variable, extractTypeStatementFrom(flow, variable))?.takeIf { it.isNotEmpty }
            }
        return buildSmartCastStatement(flow, variable, typeStatement)
    }

    open fun extractTypeStatementFrom(flow: Flow, variable: DataFlowVariable): TypeStatement? = flow.getTypeStatement(variable)

    fun buildSmartCastStatement(
        flow: Flow,
        variable: DataFlowVariable,
        typeStatement: TypeStatement?,
    ): SmartCastStatement? {
        val upperTypes = typeStatement?.upperTypes
        val upperTypesStability = when {
            upperTypes != null -> variable.getStability(flow, targetTypes = upperTypes)
            else -> SmartcastStability.STABLE_VALUE
        }
        // If there are any assignments to local variables, we can no longer guarantee
        // that the lower type bound is correct.
        val stabilityWithNoTargetTypes = variable.getStability(flow, targetTypes = null)
        val lowerTypes = typeStatement?.lowerTypes
        val lowerTypesFromVariable = when (val unwrapped = flow.unwrapVariable(variable)) {
            variable -> null // Stay conservative and don't infer anything for literal accesses to enums - only to variables aliasing them
            else -> inferLowerTypesFromVariable(unwrapped)
        }
        return if (
            upperTypes?.isNotEmpty() == true ||
            lowerTypes?.isNotEmpty() == true ||
            lowerTypesFromVariable?.isNotEmpty() == true
        ) {
            SmartCastStatement(
                upperTypes.orEmpty(), upperTypesStability,
                lowerTypes.orEmpty() + lowerTypesFromVariable.orEmpty(), stabilityWithNoTargetTypes,
            )
        } else {
            null
        }
    }

    private fun inferLowerTypesFromVariable(variable: DataFlowVariable): Set<DfaType>? {
        val symbol = (variable as? RealVariable)?.symbol as? FirEnumEntrySymbol ?: return null
        return inferLowerTypesFromSymbol(symbol)
    }

    private fun inferLowerTypesFromSymbol(symbol: FirEnumEntrySymbol): Set<DfaType>? =
        with(components) { symbol.getComplementarySymbols() }
            ?.takeIf { it.isNotEmpty() }
            ?.mapTo(mutableSetOf(), DfaType::Symbol)

    data class SmartCastStatement(
        val upperTypes: Set<ConeKotlinType>,
        val upperTypesStability: SmartcastStability,
        val lowerTypes: Set<DfaType>,
        val lowerTypesStability: SmartcastStability,
    )

    fun returnExpressionsOfAnonymousFunction(function: FirAnonymousFunction): Collection<FirAnonymousFunctionReturnExpressionInfo> =
        graphBuilder.returnExpressionsOfAnonymousFunction(function) ?: error("anonymous function ${function.render()} not analyzed")

    // ----------------------------------- Named function -----------------------------------

    fun enterFunction(function: FirFunction) {
        if (function is FirDefaultPropertyAccessor) return

        val assignedInside = context.variableAssignmentAnalyzer.enterFunction(function)

        val (localFunctionNode, functionEnterNode) = if (function is FirAnonymousFunction) {
            null to graphBuilder.enterAnonymousFunction(function)
        } else {
            graphBuilder.enterFunction(function)
        }
        localFunctionNode?.mergeIncomingFlow()
        functionEnterNode.mergeIncomingFlow { _, flow ->
            if (function is FirAnonymousFunction) {
                /*
             * Anonymous functions which can be revisited, either in-place or not in-place, are treated as repeatable statements. This
             * causes any assignments to local variables within the anonymous function body to clear type statements for those local
             * variables.
             */
                if (function.invocationKind?.canBeRevisited() != false) {
                    enterRepeatableStatement(flow, assignedInside)
                }
                function.lambdaArgumentParent?.let {
                    processConditionalContract(flow, it, null, targetLambdaArgument = function)
                }
            }
        }
    }

    fun exitFunction(function: FirFunction): FirControlFlowGraphReference? {
        if (function is FirDefaultPropertyAccessor) return null

        context.variableAssignmentAnalyzer.exitFunction()

        if (function is FirAnonymousFunction) {
            val (functionExitNode, postponedLambdaExitNode, graph) = graphBuilder.exitAnonymousFunction(function)
            functionExitNode.mergeIncomingFlow()
            postponedLambdaExitNode?.mergeIncomingFlow()
            resetSmartCastPosition() // roll back to state before function
            return FirControlFlowGraphReferenceImpl(graph)
        }

        val (node, graph) = graphBuilder.exitFunction(function)
        node.mergeIncomingFlow()
        graph.completePostponedNodes()
        resetSmartCastPosition()
        return FirControlFlowGraphReferenceImpl(graph)
    }

    // ----------------------------------- Anonymous function -----------------------------------

    fun enterAnonymousFunctionExpression(anonymousFunctionExpression: FirAnonymousFunctionExpression) {
        val (expressionNode, captureNode) = graphBuilder.enterAnonymousFunctionExpression(anonymousFunctionExpression)
        captureNode?.mergeIncomingFlow()
        expressionNode?.mergeIncomingFlow()
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
            resetSmartCastPosition()
        }
        graph?.completePostponedNodes()
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
            resetSmartCastPosition() // to state before class initialization
        }
        graph?.completePostponedNodes()
        return graph
    }

    fun exitAnonymousObjectExpression(anonymousObjectExpression: FirAnonymousObjectExpression) {
        graphBuilder.exitAnonymousObjectExpression(anonymousObjectExpression)?.mergeIncomingFlow()
    }

    // ----------------------------------- Scripts ------------------------------------------

    fun enterScript(script: FirScript, buildGraph: Boolean) {
        graphBuilder.enterScript(script, buildGraph)?.mergeIncomingFlow()
    }

    fun exitScript(): ControlFlowGraph? {
        val (node, graph) = graphBuilder.exitScript()
        node?.mergeIncomingFlow()
        graph?.completePostponedNodes()
        return graph
    }

    // ----------------------------------- Code Fragment ------------------------------------------

    fun enterCodeFragment(codeFragment: FirCodeFragment) {
        context.variableAssignmentAnalyzer.enterCodeFragment(codeFragment)
        graphBuilder.enterCodeFragment(codeFragment).mergeIncomingFlow { _, flow ->
            val smartCasts = codeFragment.codeFragmentContext?.smartCasts.orEmpty()
            for ((realVariable, exactTypes) in smartCasts) {
                flow.addTypeStatement(
                    PersistentTypeStatement(
                        variableStorage.remember(realVariable),
                        upperTypes = exactTypes.toPersistentSet(),
                        lowerTypes = persistentSetOf(),
                    )
                )
            }
        }
    }

    fun exitCodeFragment(codeFragment: FirCodeFragment): ControlFlowGraph {
        context.variableAssignmentAnalyzer.exitCodeFragment(codeFragment)
        val (node, graph) = graphBuilder.exitCodeFragment()
        node.mergeIncomingFlow()
        graph.completePostponedNodes()
        return graph
    }

    // ----------------------------------- REPL Snippet ------------------------------------------

    fun enterReplSnippet(snippet: FirReplSnippet, buildGraph: Boolean) {
        context.variableAssignmentAnalyzer.enterReplSnippet(snippet)
        graphBuilder.enterReplSnippet(snippet, buildGraph)?.mergeIncomingFlow()
    }

    fun exitReplSnippet(snippet: FirReplSnippet): ControlFlowGraph? {
        context.variableAssignmentAnalyzer.exitReplSnippet(snippet)
        val (node, graph) = graphBuilder.exitReplSnippet()
        node?.mergeIncomingFlow()
        graph?.completePostponedNodes()
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
        graph.completePostponedNodes()
        return graph
    }

    // ----------------------------------- Property -----------------------------------

    fun enterProperty(property: FirProperty) {
        // For REPL properties, the variable enter node helps include the initializer graph in the
        // eval function body graph.
        val (variableEnter, initializerEnter) = graphBuilder.enterProperty(property) ?: return
        variableEnter?.mergeIncomingFlow()
        initializerEnter.mergeIncomingFlow()
    }

    fun exitProperty(property: FirProperty, hadExplicitType: Boolean): ControlFlowGraph? {
        // For REPL properties, the variable exit node helps include the initializer graph in the
        // eval function body graph. It also provides a location to include data-flow information
        // from the resolved initializer type.
        val (initializerExit, variableExit, graph) = graphBuilder.exitProperty(property) ?: return null
        initializerExit.mergeIncomingFlow()
        variableExit?.mergeIncomingFlow { _, flow ->
            val initializer = property.initializer ?: return@mergeIncomingFlow
            exitVariableInitialization(flow, initializer, property, assignmentLhs = null, hadExplicitType)
        }
        graph.completePostponedNodes()
        return graph
    }

    // ----------------------------------- Field -----------------------------------

    fun enterField(field: FirField) {
        graphBuilder.enterField(field)?.mergeIncomingFlow()
    }

    fun exitField(field: FirField): ControlFlowGraph? {
        val (node, graph) = graphBuilder.exitField(field) ?: return null
        node.mergeIncomingFlow()
        graph.completePostponedNodes()
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
        val operandVariable = flow.getVariableIfUsedOrReal(typeOperatorCall.argument) ?: return
        val complementarySymbols = typeOperatorCall.conversionTypeRef.coneType
                .toRegularClassSymbol()?.getComplementarySymbols()?.takeIf { it.isNotEmpty() }
        when (val operation = typeOperatorCall.operation) {
            FirOperation.IS, FirOperation.NOT_IS -> {
                val isType = operation == FirOperation.IS
                when (type) {
                    // x is Nothing? <=> x == null
                    nullableNothing -> processEqNull(flow, typeOperatorCall, typeOperatorCall.argument, isType)
                    // x is Any <=> x != null
                    any -> processEqNull(flow, typeOperatorCall, typeOperatorCall.argument, !isType)
                    else -> {
                        val expressionVariable = SyntheticVariable(typeOperatorCall)
                        if (operandVariable.isReal()) {
                            flow.addImplication((expressionVariable eq isType) implies (operandVariable typeEq type))
                            flow.addImplication((expressionVariable eq !isType) implies (operandVariable typeNotEq type))

                            if (complementarySymbols != null) {
                                flow.addImplication((expressionVariable eq isType) implies (operandVariable valueNotEq complementarySymbols))
                            }
                        }
                        if (!type.canBeNull(components.session)) {
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

                    if (complementarySymbols != null) {
                        flow.addTypeStatement(operandVariable valueNotEq complementarySymbols)
                    }
                }
                if (!type.canBeNull(components.session)) {
                    flow.commitOperationStatement(operandVariable notEq null)
                } else {
                    val expressionVariable = SyntheticVariable(typeOperatorCall)
                    flow.addImplication((expressionVariable notEq null) implies (operandVariable notEq null))
                    flow.addImplication((expressionVariable eq null) implies (operandVariable eq null))
                }
            }

            FirOperation.SAFE_AS -> {
                val expressionVariable = SyntheticVariable(typeOperatorCall)
                flow.addImplication((expressionVariable notEq null) implies (operandVariable notEq null))
                if (operandVariable.isReal()) {
                    flow.addImplication((expressionVariable notEq null) implies (operandVariable typeEq type))

                    if (complementarySymbols != null) {
                        flow.addImplication((expressionVariable notEq null) implies (operandVariable valueNotEq complementarySymbols))
                    }
                }
            }

            else -> throw IllegalStateException()
        }
    }

    fun exitComparisonExpressionCall(comparisonExpression: FirComparisonExpression) {
        graphBuilder.exitComparisonExpression(comparisonExpression).mergeIncomingFlow()
    }

    fun enterEqualityOperatorCall() {
        graphBuilder.enterCall()
    }

    fun exitEqualityOperatorLhs() {
        graphBuilder.exitEqualityOperatorLhs()
    }

    fun exitEqualityOperatorCall(equalityOperatorCall: FirEqualityOperatorCall, callCompleted: Boolean) {
        val (lhsExitNode, node) = graphBuilder.exitEqualityOperatorCall(equalityOperatorCall, callCompleted)
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
            is FirWhenSubjectExpression -> leftOperand.whenSubject
            else -> leftOperand
        } as? FirLiteralExpression
        val rightConst = rightOperand as? FirLiteralExpression
        val leftIsNullConst = leftConst?.kind == ConstantValueKind.Null
        val rightIsNullConst = rightConst?.kind == ConstantValueKind.Null
        val leftIsNull = leftIsNullConst || leftOperand.resolvedType.isNullableNothing && !rightIsNullConst
        val rightIsNull = rightIsNullConst || rightOperand.resolvedType.isNullableNothing && !leftIsNullConst

        node.mergeIncomingFlow { _, flow ->
            if (leftIsNull || leftConst != null || rightIsNull || rightConst != null) {
                when {
                    leftIsNull -> processEqNull(flow, equalityOperatorCall, rightOperand, operation.isEq(), lhsExitNode.flow)
                    leftConst != null -> processEqConst(flow, equalityOperatorCall, rightOperand, leftConst, operation.isEq())
                }
                when {
                    rightIsNull -> processEqNull(flow, equalityOperatorCall, leftOperand, operation.isEq(), lhsExitNode.flow)
                    rightConst != null -> processEqConst(flow, equalityOperatorCall, leftOperand, rightConst, operation.isEq())
                }
            } else {
                processEq(flow, lhsExitNode.flow, equalityOperatorCall, leftOperand, rightOperand, operation)
            }
        }
    }

    private fun processEqConst(
        flow: MutableFlow,
        expression: FirEqualityOperatorCall,
        operand: FirExpression,
        const: FirLiteralExpression,
        isEq: Boolean,
    ) {
        if (const.kind == ConstantValueKind.Null) {
            return processEqNull(flow, expression, operand, isEq)
        }

        val operandVariable = flow.getVariableIfUsedOrReal(operand) ?: return
        val expressionVariable = SyntheticVariable(expression)

        if (const.kind == ConstantValueKind.Boolean && operand.resolvedType.isBooleanOrNullableBoolean) {
            val expected = (const.value as Boolean)
            flow.addImplication((expressionVariable eq isEq) implies (operandVariable eq expected))
            if (operand.resolvedType.isBoolean) {
                flow.addImplication((expressionVariable eq !isEq) implies (operandVariable eq !expected))
            }

            if (operandVariable is RealVariable) {
                flow.addImplication((expressionVariable eq !isEq) implies (operandVariable valueNotEq expected))
            }
        } else {
            // expression == non-null const -> expression != null
            flow.addImplication((expressionVariable eq isEq) implies (operandVariable notEq null))
        }

        // We can imply type information if the constant is the left operand and is a supported primitive type.
        if (operandVariable is RealVariable && const == expression.arguments[0] && isSmartcastPrimitive(const.resolvedType.classId)) {
            flow.addImplication((expressionVariable eq isEq) implies (operandVariable typeEq const.resolvedType))
        }
    }

    private fun processEqNull(
        flow: MutableFlow,
        expression: FirExpression,
        operand: FirExpression,
        isEq: Boolean,
        lhsExitFlow: PersistentFlow? = null,
    ) {
        val operandVariable = flow.getVariableIfUsedOrReal(operand) ?: return
        if (operandVariable is RealVariable && lhsExitFlow != null &&
            !logicSystem.isSameValueIn(lhsExitFlow, flow, operandVariable)
        ) return

        val expressionVariable = SyntheticVariable(expression)
        flow.addImplication((expressionVariable eq isEq) implies (operandVariable eq null))
        flow.addImplication((expressionVariable eq !isEq) implies (operandVariable notEq null))
    }

    private fun processEq(
        flow: MutableFlow,
        lhsExitFlow: PersistentFlow,
        expression: FirEqualityOperatorCall,
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

        // Only consider the LHS variable if it has not been reassigned in the RHS.
        val leftOperandVariable = flow.getVariableIfUsedOrReal(leftOperand)
            .takeIf { isSameValueIn(lhsExitFlow, leftOperand, flow) }
        val rightOperandVariable = flow.getVariableIfUsedOrReal(rightOperand)
        if (leftOperandVariable == null && rightOperandVariable == null) return
        val expressionVariable = SyntheticVariable(expression)

        if (leftIsNullable || rightIsNullable) {
            // `a == b:Any` => `a != null`; the inverse is not true - we don't know when `a` *is* `null`
            val nullableOperand = if (leftIsNullable) leftOperandVariable else rightOperandVariable
            if (nullableOperand != null) {
                flow.addImplication((expressionVariable eq isEq) implies (nullableOperand notEq null))
            }
        }

        if (leftOperandVariable !is RealVariable && rightOperandVariable !is RealVariable) return

        val equalsContract = when {
            operation != FirOperation.EQ && operation != FirOperation.NOT_EQ -> EqualsOverrideContract.SAFE_FOR_SMART_CAST
            else -> computeEqualsOverrideContract(leftOperandType, components.session, components.scopeSession, trustExpectClasses = false)
        }

        fun FirBasedSymbol<*>.isSingleton(): Boolean = this is FirEnumEntrySymbol
                // If the object has a problematic `equals()`, it will be reported during `when` exhaustiveness analysis.
                || this is FirRegularClassSymbol && classKind.isObject

        fun addEqualityImplications(variable: DataFlowVariable?, otherOperand: FirExpression) {
            if (variable !is RealVariable) return

            if (equalsContract == EqualsOverrideContract.SAFE_FOR_SMART_CAST) {
                flow.addImplication((expressionVariable eq isEq) implies (variable typeEq otherOperand.resolvedType))
            }

            val symbol = when (val other = otherOperand.unwrapSmartcastExpression()) {
                is FirPropertyAccessExpression -> other.calleeReference.toResolvedBaseSymbol()?.takeIf { it.isSingleton() }
                is FirResolvedQualifier -> other.symbol?.takeIf { it.isSingleton() }
                else -> null
            }
            if (symbol != null) {
                flow.addImplication((expressionVariable eq !isEq) implies (variable valueNotEq symbol))
            }
            val complementarySymbols = when (symbol) {
                is FirEnumEntrySymbol -> with(components) { symbol.getComplementarySymbols() }
                is FirRegularClassSymbol if symbol.classKind.isObject -> with(components) { symbol.getComplementarySymbols() }
                else -> null
            }
            if (complementarySymbols != null && complementarySymbols.isNotEmpty()) {
                flow.addImplication((expressionVariable eq isEq) implies (variable valueNotEq complementarySymbols))
            }
        }

        addEqualityImplications(leftOperandVariable, rightOperand)
        addEqualityImplications(rightOperandVariable, leftOperand)
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
            val argumentVariable = flow.getVariableIfUsedOrReal(checkNotNullCall.argument) ?: return@mergeIncomingFlow
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
        val previousConditionVariable = flow.getVariableIfUsed(previousCondition) ?: return@mergeIncomingFlow
        flow.commitOperationStatement(previousConditionVariable eq false)
    }

    fun exitWhenBranchCondition(whenBranch: FirWhenBranch) {
        val (conditionExitNode, resultEnterNode) = graphBuilder.exitWhenBranchCondition(whenBranch)
        conditionExitNode.mergeIncomingFlow()
        resultEnterNode.mergeIncomingFlow { _, flow ->
            // If the condition is invalid, don't generate smart casts to Any or Boolean.
            if (whenBranch.condition.resolvedType.isBoolean) {
                val conditionVariable = flow.getVariableIfUsed(whenBranch.condition) ?: return@mergeIncomingFlow
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
        val assignedInside = context.variableAssignmentAnalyzer.enterLoop(loop)
        val (loopEnterNode, loopConditionEnterNode) = graphBuilder.enterWhileLoop(loop)
        loopEnterNode.mergeIncomingFlow()
        loopConditionEnterNode.mergeIncomingFlow { _, flow -> enterRepeatableStatement(flow, assignedInside) }
    }

    fun exitWhileLoopCondition(loop: FirLoop) {
        val (loopConditionExitNode, loopBlockEnterNode) = graphBuilder.exitWhileLoopCondition(loop)
        loopConditionExitNode.mergeIncomingFlow()
        loopBlockEnterNode.mergeIncomingFlow { _, flow ->
            if (loop.condition.resolvedType.isBoolean) {
                val conditionVariable = flow.getVariableIfUsed(loop.condition) ?: return@mergeIncomingFlow
                flow.commitOperationStatement(conditionVariable eq true)
            }
        }
    }

    fun exitWhileLoop(loop: FirLoop) {
        val assignedInside = context.variableAssignmentAnalyzer.exitLoop()
        val (conditionEnterNode, blockExitNode, exitNode) = graphBuilder.exitWhileLoop(loop)
        blockExitNode.mergeIncomingFlow()
        exitNode.mergeIncomingFlow { path, flow ->
            processWhileLoopExit(path, flow, exitNode, conditionEnterNode, assignedInside)
            processLoopExit(flow, exitNode, exitNode.firstPreviousNode as LoopConditionExitNode)
        }
    }

    private fun processWhileLoopExit(
        path: FlowPath,
        flow: MutableFlow,
        node: LoopExitNode,
        conditionEnterNode: LoopConditionEnterNode,
        reassigned: Set<FirPropertySymbol>,
    ) {
        if (reassigned.isEmpty()) return
        // While analyzing the loop we might have added some backwards jumps to `conditionEnterNode` which weren't
        // there at the time its flow was computed - which is why we erased all information about `possiblyChangedVariables`
        // from it. Now that we have those edges, we can restore type information for the code after the loop.
        val conditionEnterFlow = conditionEnterNode.getFlow(path)
        val loopEnterAndContinueFlows = conditionEnterNode.previousLiveNodes.map { it.getFlow(path) }
        val conditionExitAndBreakFlows = node.previousLiveNodes.map { it.getFlow(path) }
        reassigned.forEach { symbol ->
            val variable = getLocal(symbol, create = false) ?: return@forEach
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
            val variable = flow.getVariableIfUsed(conditionExitNode.fir) ?: return
            flow.commitOperationStatement(variable eq false)
        }
    }

    private fun enterRepeatableStatement(flow: MutableFlow, reassigned: Set<FirPropertySymbol>) {
        for (symbol in reassigned) {
            val variable = getLocal(symbol, create = false) ?: continue
            logicSystem.recordNewAssignment(flow, variable, context.newAssignmentIndex())
        }
    }

    // ----------------------------------- Do while Loop -----------------------------------

    fun enterDoWhileLoop(loop: FirLoop) {
        val assignedInside = context.variableAssignmentAnalyzer.enterLoop(loop)
        val (loopEnterNode, loopBlockEnterNode) = graphBuilder.enterDoWhileLoop(loop)
        loopEnterNode.mergeIncomingFlow { _, flow -> enterRepeatableStatement(flow, assignedInside) }
        loopBlockEnterNode.mergeIncomingFlow()
    }

    fun enterDoWhileLoopCondition(loop: FirLoop) {
        val (loopBlockExitNode, loopConditionEnterNode) = graphBuilder.enterDoWhileLoopCondition(loop)
        loopBlockExitNode.mergeIncomingFlow()
        loopConditionEnterNode.mergeIncomingFlow()
    }

    fun exitDoWhileLoop(loop: FirLoop) {
        context.variableAssignmentAnalyzer.exitLoop()
        val (loopConditionExitNode, loopExitNode) = graphBuilder.exitDoWhileLoop(loop)
        loopConditionExitNode.mergeIncomingFlow()
        loopExitNode.mergeIncomingFlow { _, flow ->
            processLoopExit(flow, loopExitNode, loopConditionExitNode)
        }
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
            processConditionalContract(flow, qualifiedAccessExpression, callArgsExit = null)
            processBackingFieldAccess(flow, qualifiedAccessExpression)
        }
    }

    fun exitSmartCastExpression(smartCastExpression: FirSmartCastExpression) {
        graphBuilder.exitSmartCastExpression(smartCastExpression).mergeIncomingFlow()
    }

    fun enterSafeCallAfterNullCheck(safeCall: FirSafeCallExpression) {
        graphBuilder.enterSafeCall(safeCall).mergeIncomingFlow { _, flow ->
            val receiverVariable = flow.getVariableIfUsedOrReal(safeCall.receiver) ?: return@mergeIncomingFlow
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
            val expressionVariable = flow.getOrCreateVariable(safeCall) ?: return@mergeIncomingFlow
            val previousFlow = node.lastPreviousNode.getFlow(path)

            flow.addAllConditionally(expressionVariable notEq null, previousFlow)

            /*
             * If we have some implication about rhs of safe call in the previous flow, then we can expand them to the whole
             *   safe call variable
             *
             * a?.foo() // original call
             * subj.foo() // rhs of safe call
             *
             * previousFlow:
             *  - subj.foo() == True -> X_1
             *  - subj.foo() == False -> X_2
             *  - subj.foo() != Null -> X_3
             *  - subj.foo() == Null -> X_4
             *
             * flow:
             *  - a?.foo() == True -> X_1
             *  - a?.foo() == False -> X_2
             *  - a?.foo() != Null -> X_3
             *
             * Note that we don't pass implication with 'subj.foo() == Null' in the condition because there are two different ways
             *   why `a?.foo()` may be `null` -- it's either `a` is `null` or `subj.foo()` is `null`, and we can't differentiate between
             *   them
             *
             * Also, an implementation note: in the following lines we use `expressionVariable` made on safe call expression when looking
             *   for implications from previous flow in the subject, because VariableStorage doesn't differ between the whole safe call
             *   and synthetically generated selector, see [variableStorage.get] implementation
             */
            previousFlow.getImplications(expressionVariable)?.forEach {
                if (it.condition.operation != Operation.EqNull) {
                    flow.addImplication(it)
                }
            }
        }
    }

    fun exitResolvedQualifierNode(resolvedQualifier: FirResolvedQualifier) {
        graphBuilder.exitResolvedQualifierNode(resolvedQualifier).mergeIncomingFlow()
    }

    fun enterCallArguments(call: FirStatement, arguments: List<FirExpression>) {
        val lambdas = arguments.mapNotNull { it.unwrapAnonymousFunctionExpression() }
        graphBuilder.enterCall(lambdas.mapTo(mutableSetOf()) { it.symbol })
        context.variableAssignmentAnalyzer.enterFunctionCall(lambdas)
        graphBuilder.enterCallArguments(call, lambdas)?.mergeIncomingFlow()
    }

    fun exitCallArguments() {
        val (splitNode, exitNode) = graphBuilder.exitCallArguments()
        splitNode?.mergeIncomingFlow()

        if (exitNode != null) {
            exitNode.mergeIncomingFlow()

            // Reset implicit receivers back to their state *before* call arguments but after explicit receiver
            // as tower resolve will use receiver types to lookup functions after call arguments have been processed.
            // TODO(KT-64094): Consider moving logic to tower resolution instead.
            resetSmartCastPositionTo(exitNode.explicitReceiverExitNode.flow)
        }
    }

    fun exitCallExplicitReceiver() {
        graphBuilder.exitCallExplicitReceiver()
    }

    fun enterFunctionCall(functionCall: FirFunctionCall) {
        val enterNode = graphBuilder.enterFunctionCall(functionCall)
        enterNode.mergeIncomingFlow()
    }

    fun exitFunctionCall(functionCall: FirFunctionCall, callCompleted: Boolean) {
        context.variableAssignmentAnalyzer.exitFunctionCall(callCompleted)
        val node = graphBuilder.exitFunctionCall(functionCall, callCompleted)
        node.mergeIncomingFlow { _, flow ->
            val callArgsExit = node.previousNodes.singleOrNull { it is FunctionCallEnterNode }
            processConditionalContract(flow, functionCall, callArgsExit?.flow)
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

    /**
     * Receiver, value arguments, context arguments
     */
    private fun FirStatement.orderedArguments(callee: FirFunction): Array<out FirExpression?>? {
        fun FirQualifiedAccessExpression.firstReceiver(): FirExpression? {
            val candidate = candidate()
            // Processing case with a candidate might be necessary for PCLA, because even top-level calls might be not fully completed
            if (candidate != null) {
                return candidate.chosenExtensionReceiverExpression() ?: candidate.dispatchReceiverExpression()
            }
            return extensionReceiver ?: dispatchReceiver
        }

        fun FirQualifiedAccessExpression.addContextArgumentsTo(target: MutableList<FirExpression?>) {
            val candidate = candidate()
            if (candidate != null) {
                candidate.contextArguments?.forEach { target.add(it.expression) }
            } else {
                contextArguments.forEach { target.add(it) }
            }
        }

        return buildList {
            val receiver = when (this@orderedArguments) {
                is FirQualifiedAccessExpression -> firstReceiver()
                is FirVariableAssignment -> (lValue as? FirQualifiedAccessExpression)?.firstReceiver()
                else -> null
            }
            add(receiver)

            if (this@orderedArguments is FirFunctionCall) {
                val argumentToParameter = resolvedArgumentMapping ?: candidate()?.argumentMapping?.unwrapAtoms() ?: return null
                val parameterToArgument = argumentToParameter.entries.associate { it.value to it.key.unwrapArgument() }
                callee.valueParameters.forEach { add(parameterToArgument[it]) }
            }

            if (this@orderedArguments is FirQualifiedAccessExpression) {
                addContextArgumentsTo(this)
            } else if (this@orderedArguments is FirVariableAssignment) {
                val lValue = lValue as? FirQualifiedAccessExpression
                lValue?.addContextArgumentsTo(this)
                add(rValue)
            }
        }.toTypedArray()
    }

    private fun processConditionalContract(
        flow: MutableFlow,
        qualifiedAccess: FirStatement,
        callArgsExit: PersistentFlow?,
        targetLambdaArgument: FirAnonymousFunction? = null, // entering lambda argument where we should process holdsIn contracts only
        // TODO: split or rewrite to simplify the cases, see KT-78107
    ) {
        // contracts has no effect on non-body resolve stages
        if (!components.transformer.baseTransformerPhase.isBodyResolve) return

        val callee: FirFunction = when (qualifiedAccess) {
            is FirFunctionCall -> qualifiedAccess.calleeReference.symbol?.fir as? FirNamedFunction
            is FirQualifiedAccessExpression -> qualifiedAccess.calleeReference.symbol?.let { it.fir as? FirProperty }?.getter
            is FirVariableAssignment -> qualifiedAccess.calleeReference?.symbol?.let { it.fir as? FirProperty }?.setter
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

        val arguments = qualifiedAccess.orderedArguments(callee) ?: return

        var hasAnyContractsToProcess = false
        val conditionalEffects: MutableList<ConeConditionalEffectDeclaration> = mutableListOf()
        val conditionalReturns: MutableList<ConeConditionalReturnsDeclaration> = mutableListOf()
        val conditionalHoldsIn: MutableList<ConeHoldsInEffectDeclaration> = mutableListOf()

        val indexOfLambdaArgument =
            if (targetLambdaArgument == null) -1
            // this is quite fragile, but -1 is because `orderedArguments` adds the receiver to the list
            else arguments.indexOfFirst { (it as? FirAnonymousFunctionExpression)?.anonymousFunction == targetLambdaArgument } - 1

        contractDescription.effects.forEach {
            val effect = it.effect
            when {
                targetLambdaArgument == null && effect is ConeConditionalEffectDeclaration -> {
                    conditionalEffects.add(effect)
                    hasAnyContractsToProcess = true
                }
                targetLambdaArgument == null && effect is ConeConditionalReturnsDeclaration -> {
                    conditionalReturns.add(effect)
                    hasAnyContractsToProcess = true
                }
                targetLambdaArgument != null && effect is ConeHoldsInEffectDeclaration
                    && effect.valueParameterReference.parameterIndex == indexOfLambdaArgument ->
                {
                    conditionalHoldsIn.add(effect)
                    hasAnyContractsToProcess = true
                }
            }
        }
        if (!hasAnyContractsToProcess) return

        val argumentVariablesForConditionalEffects = Array(arguments.size) { i ->
            arguments[i]?.let { argument ->
                flow.getVariableIfUsedOrReal(argument)
                    // Only apply contract information to argument if it has not been reassigned in a lambda.
                    .takeIf { callArgsExit == null || isSameValueIn(callArgsExit, argument, flow) }
            }
        }
        val allArgumentVariables = Array(arguments.size) { i ->
            arguments[i]?.let { argument -> flow.getOrCreateVariable(argument) }
        }

        val substitutor = getSubstitutor(callee, qualifiedAccess, originalFunction)

        fun processEffect(condition: ConeBooleanExpression, operation: Operation?) {
            val statements =
                logicSystem.approveContractStatement(condition, argumentVariablesForConditionalEffects, substitutor) {
                    logicSystem.approveOperationStatement(flow, it, removeApprovedOrImpossible = operation == null)
                } ?: return // TODO: do what if the result is known to be false?
            if (operation == null) {
                flow.addAllStatements(statements)
            } else if (qualifiedAccess is FirExpression) {
                val functionCallVariable = flow.getOrCreateVariable(qualifiedAccess)
                if (functionCallVariable != null) {
                    flow.addAllConditionally(OperationStatement(functionCallVariable, operation), statements)
                }
            }
        }

        if (argumentVariablesForConditionalEffects.any { it != null }) {
            if (targetLambdaArgument == null) {
                for (conditionalEffect in conditionalEffects) {
                    val effect = conditionalEffect.effect as? ConeReturnsEffectDeclaration ?: continue
                    val operation = effect.value.toOperation()
                    processEffect(conditionalEffect.condition, operation)
                }
            } else {
                for (conditionHoldsIn in conditionalHoldsIn) {
                    processEffect(conditionHoldsIn.argumentsCondition, null)
                }
            }
        }

        if (targetLambdaArgument == null) {
            if (qualifiedAccess is FirExpression) {
                for (conditionalReturn in conditionalReturns) {
                    val effect = conditionalReturn.returnsEffect as? ConeReturnsEffectDeclaration ?: continue
                    if (effect.value != ConeContractConstantValues.NOT_NULL) continue
                    val statements =
                        logicSystem.approveContractStatement(
                            conditionalReturn.argumentsCondition, allArgumentVariables, substitutor, typesOnlyFromRealVars = false
                        ) {
                            logicSystem.approveOperationStatement(flow, it, removeApprovedOrImpossible = true)
                        }
                    statements?.forEach { (_, statement) ->
                        val approved = logicSystem.approveTypeStatement(flow, statement)
                        if (approved) {
                            val functionReturnCondition = OperationStatement(SyntheticVariable(qualifiedAccess), Operation.NotEqNull)
                            val functionReturnStatements =
                                logicSystem.approveOperationStatement(flow, functionReturnCondition, removeApprovedOrImpossible = true)
                            flow.addAllStatements(functionReturnStatements)
                        }
                    }
                }
            }
        }
    }

    private fun processBackingFieldAccess(flow: MutableFlow, qualifiedAccess: FirQualifiedAccessExpression) {
        val callee = qualifiedAccess.calleeReference as? FirPropertyWithExplicitBackingFieldResolvedNamedReference ?: return
        val fieldSymbol = callee.tryAccessExplicitFieldSymbol(components.context.publicApiInlineFunction, session) ?: return
        if (isPrivateToThisInvisibleAccess(qualifiedAccess, session, fieldSymbol)) return
        val variable = flow.getOrCreateVariable(qualifiedAccess) ?: return
        val returnType = components.returnTypeCalculator.tryCalculateReturnType(fieldSymbol).coneType
        flow.addTypeStatement(variable typeEq returnType)
    }

    private fun getSubstitutor(
        callee: FirFunction,
        qualifiedAccess: FirStatement,
        originalFunction: FirFunction?,
    ): ConeSubstitutor {
        val typeParameters = callee.typeParameters
        val typeArgumentsSubstitutor = if (typeParameters.isNotEmpty() && qualifiedAccess is FirQualifiedAccessExpression) {
            @Suppress("UNCHECKED_CAST")
            val substitutionFromArguments = typeParameters.zip(qualifiedAccess.typeArguments).map { (typeParameterRef, typeArgument) ->
                typeParameterRef.symbol to typeArgument.toConeTypeProjection().type
            }.filter { it.second != null }.toMap() as Map<FirTypeParameterSymbol, ConeKotlinType>
            substitutorByMap(substitutionFromArguments, components.session)
        } else {
            ConeSubstitutor.Empty
        }
        return if (originalFunction == null) {
            typeArgumentsSubstitutor
        } else {
            val map = originalFunction.symbol.typeParameterSymbols.zip(typeParameters.map { it.symbol.toConeType() }).toMap()
            substitutorByMap(map, components.session).chain(typeArgumentsSubstitutor)
        }
    }

    fun exitLiteralExpression(literalExpression: FirLiteralExpression) {
        if (literalExpression.hasResolvedType) return
        graphBuilder.exitLiteralExpression(literalExpression).mergeIncomingFlow()
    }

    fun enterLocalVariableDeclaration(variable: FirProperty) {
        graphBuilder.enterVariableDeclaration(variable).mergeIncomingFlow()
    }

    fun exitLocalVariableDeclaration(variable: FirProperty, hadExplicitType: Boolean) {
        graphBuilder.exitVariableDeclaration(variable).mergeIncomingFlow { _, flow ->
            val initializer = variable.initializer ?: return@mergeIncomingFlow
            exitVariableInitialization(flow, initializer, variable, assignmentLhs = null, hadExplicitType)
        }
    }

    fun exitVariableAssignment(assignment: FirVariableAssignment) {
        val property = assignment.calleeReference?.toResolvedPropertySymbol()?.fir
        if (property != null && property.isEffectivelyLocal) {
            context.variableAssignmentAnalyzer.visitAssignment(property, assignment.rValue.resolvedType.refinedTypeForDataFlowOrSelf)
        }

        graphBuilder.exitVariableAssignment(assignment).mergeIncomingFlow { _, flow ->
            property ?: return@mergeIncomingFlow
            if (property.isEffectivelyLocal || property.isVal) {
                exitVariableInitialization(flow, assignment.rValue, property, assignment.lValue, hasExplicitType = false)
            } else {
                val variable = flow.getRealVariableWithoutUnwrappingAlias(assignment.lValue)
                if (variable != null) {
                    logicSystem.recordNewAssignment(flow, variable, context.newAssignmentIndex())
                }
            }
            processConditionalContract(flow, assignment, callArgsExit = null)
        }
    }

    private fun exitVariableInitialization(
        flow: MutableFlow,
        initializer: FirExpression,
        property: FirProperty,
        assignmentLhs: FirExpression?,
        hasExplicitType: Boolean,
    ) {
        val propertyVariable = if (assignmentLhs != null) {
            flow.getVariableWithoutUnwrappingAlias(assignmentLhs, createReal = true) as? RealVariable ?: return
        } else {
            getLocal(property.symbol, create = true) ?: return
        }
        val isAssignment = assignmentLhs != null
        if (isAssignment) {
            logicSystem.recordNewAssignment(flow, propertyVariable, context.newAssignmentIndex())
        }
        var needToAddInitializerStatement = isAssignment

        val stability = propertyVariable.getStability(flow, components.session)
        if (stability == SmartcastStability.STABLE_VALUE || stability == SmartcastStability.CAPTURED_VARIABLE) {
            val initializerVariable = flow.getVariableIfUsedOrReal(initializer)
            if (!hasExplicitType && initializerVariable is RealVariable &&
                // It's impossible to reference implicit when subjects.
                // With explicit local variables, we want to give the user an option to
                // choose whether they want to access the variable with smartcasts
                // or the original expression without them.
                (property.isImplicitWhenSubjectVariable ||
                        initializerVariable.getStability(flow, targetTypes = null) == SmartcastStability.STABLE_VALUE)
            ) {
                // val a = ...
                // val b = a
                // if (b != null) { /* a != null */ }
                logicSystem.addLocalVariableAlias(flow, propertyVariable, initializerVariable)
            } else if (initializerVariable != null && (!property.isEffectivelyLocal || !property.isVar)) {
                // Case 1:
                //   val b = x is String // initializer is synthetic, condition is boolean
                //   if (b) { /* x is String */ }
                // Case 2:
                //   val b = x?.foo() // initializer is synthetic, condition is on nullability
                //   if (b != null) { /* x != null */ }
                // Case 3:
                //   val b = x?.foo // if `foo` is mutable, then initializer is real, but unstable
                //   if (b != null) { /* x != null, but re-reading x.foo could produce null */ }
                val translateAll = LanguageFeature.DfaBooleanVariables.isEnabled()
                logicSystem.translateVariableFromConditionInStatements(flow, initializerVariable, propertyVariable) {
                    it.takeIf { translateAll || it.condition.operation == Operation.EqNull || it.condition.operation == Operation.NotEqNull }
                }
            } else {
                needToAddInitializerStatement = needToAddInitializerStatement ||
                        // required to support REPL property approximation of local types.
                        (property.isReplSnippetDeclaration == true && !hasExplicitType) ||
                        // required for "reverse" implies - returns contracts, see KT-79220
                        (initializer is FirSmartCastExpression &&
                                flow.unwrapVariable(propertyVariable).originalType != initializer.resolvedType)
            }
        }

        if (needToAddInitializerStatement) {
            // `propertyVariable` can be an alias to `initializerVariable`, in which case this will add
            // a redundant type statement which is fine...probably
            flow.addTypeStatement(flow.unwrapVariable(propertyVariable) typeEq initializer.resolvedType)
        }
    }

    fun exitThrowExceptionNode(throwExpression: FirThrowExpression) {
        graphBuilder.exitThrowExceptionNode(throwExpression).mergeIncomingFlow()
    }

    // ----------------------------------- Boolean operators -----------------------------------

    fun enterBooleanOperatorExpression(booleanOperatorExpression: FirBooleanOperatorExpression) {
        graphBuilder.enterBooleanOperatorExpression(booleanOperatorExpression).mergeIncomingFlow()
    }

    fun exitLeftBooleanOperatorExpressionArgument(booleanOperatorExpression: FirBooleanOperatorExpression) {
        val (leftExitNode, rightEnterNode) = graphBuilder.exitLeftBooleanOperatorExpressionArgument(booleanOperatorExpression)
        leftExitNode.mergeIncomingFlow()
        rightEnterNode.mergeIncomingFlow { _, flow ->
            val leftOperandVariable = flow.getVariableIfUsed(booleanOperatorExpression.leftOperand) ?: return@mergeIncomingFlow
            val saturatingValue = booleanOperatorExpression.kind != LogicOperationKind.AND
            flow.commitOperationStatement(leftOperandVariable eq !saturatingValue)
        }
    }

    fun exitBooleanOperatorExpression(booleanOperatorExpression: FirBooleanOperatorExpression) {
        graphBuilder.exitBooleanOperatorExpression(booleanOperatorExpression).mergeBooleanLogicOperatorFlow()
    }

    private fun BooleanOperatorExitNode.mergeBooleanLogicOperatorFlow() = mergeIncomingFlow { path, flow ->
        val inferMoreImplications =
            LanguageFeature.InferMoreImplicationsFromBooleanExpressions.isEnabled()

        // The saturating value is one that, when returned by any argument, also has to be returned by the entire expression:
        // `true` for `||` and `false` for `&&`.
        val saturatingValue = fir.kind != LogicOperationKind.AND
        val flowFromLeft = leftOperandNode.getFlow(path)
        val flowFromRight = rightOperandNode.getFlow(path)
        // Not checking this variable for reassignments is safe because the only statement we will approve on it is
        // `leftVariable eq saturatingValue`, which implies that the right side, along with any assignments in it,
        // did not execute at all due to short-circuiting.
        val leftVariable = flowFromLeft.takeIf { fir.leftOperand.resolvedType.isBoolean }?.getVariableIfUsed(fir.leftOperand)
        val rightVariable = flowFromRight.takeIf { fir.rightOperand.resolvedType.isBoolean }?.getVariableIfUsed(fir.rightOperand)

        when {
            // If RHS cannot terminate, then LHS *has* to be saturating, otherwise the entire expression won't terminate.
            !isDead && rightOperandNode.isDead -> {
                if (leftVariable != null) {
                    flow.commitOperationStatement(leftVariable eq saturatingValue)
                }
            }

            // Value of the expression = value of the right hand side.
            inferMoreImplications && fir.leftOperand.booleanLiteralValue == !saturatingValue -> {
                if (rightVariable != null) {
                    logicSystem.translateVariableFromConditionInStatements(flow, rightVariable, SyntheticVariable(fir))
                }
            }

            // Value of the expression = value of the left hand side.
            inferMoreImplications && fir.rightOperand.booleanLiteralValue == !saturatingValue -> {
                if (leftVariable != null) {
                    logicSystem.translateVariableFromConditionInStatements(flow, leftVariable, SyntheticVariable(fir))
                }
            }

            else -> {
                val statementsFromRight = flow.getTypeStatementsNotInheritedFrom(flowFromRight)

                // The right argument is only evaluated if the left argument is not saturating, so the statements
                // returned by this function always include those implied by `leftVariable eq !saturatingValue`.
                fun getStatementsWhenRightArgumentIs(value: Boolean) =
                    if (rightVariable != null) logicSystem.andForTypeStatements(
                        statementsFromRight,
                        logicSystem.approveOperationStatement(flowFromRight, rightVariable eq value),
                    ) else statementsFromRight

                // If the result is not saturating, then both sides executed and are not saturating.
                val whenNotSaturating = getStatementsWhenRightArgumentIs(!saturatingValue)
                // If the result is saturating, then either the left side is saturating and the right side did not execute,
                // or both sides executed, the left side is not saturating, and the right side is saturating.
                val whenSaturating = if (leftVariable != null && (rightVariable != null || inferMoreImplications)) {
                    logicSystem.orForTypeStatements(
                        logicSystem.approveOperationStatement(flowFromLeft, leftVariable eq saturatingValue),
                        if (inferMoreImplications) {
                            getStatementsWhenRightArgumentIs(saturatingValue)
                        } else {
                            logicSystem.approveOperationStatement(flowFromRight, rightVariable!! eq saturatingValue)
                        }
                    )
                } else emptyMap()
                if (inferMoreImplications) {
                    // The entire boolean expression has to be true or false, so the `or` of the two is always correct.
                    flow.addAllStatements(logicSystem.orForTypeStatements(whenSaturating, whenNotSaturating))
                }
                val operatorVariable = SyntheticVariable(fir)
                flow.addAllConditionally(operatorVariable eq saturatingValue, whenSaturating)
                flow.addAllConditionally(operatorVariable eq !saturatingValue, whenNotSaturating)
            }
        }
    }

    private fun exitBooleanNot(flow: MutableFlow, expression: FirFunctionCall) {
        // Processing case with a candidate might be necessary for PCLA, because even top-level calls might be not fully completed
        val argument = expression.candidate()?.dispatchReceiverExpression() ?: expression.dispatchReceiver!!
        val argumentVariable = flow.getVariableIfUsed(argument) ?: return
        val expressionVariable = SyntheticVariable(expression)
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
        resetSmartCastPosition() // rollback to position before annotation
    }

    // ----------------------------------- Init block -----------------------------------

    fun enterInitBlock(initBlock: FirAnonymousInitializer) {
        context.variableAssignmentAnalyzer.enterAnonymousInitializer(initBlock)
        graphBuilder.enterInitBlock(initBlock).mergeIncomingFlow()
    }

    fun exitInitBlock(initBlock: FirAnonymousInitializer): ControlFlowGraph {
        context.variableAssignmentAnalyzer.exitAnonymousInitializer(initBlock)
        val (node, controlFlowGraph) = graphBuilder.exitInitBlock()
        node.mergeIncomingFlow()
        controlFlowGraph.completePostponedNodes()
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
        lhsIsNotNullNode.mergeIncomingFlow { _, flow ->
            val lhs = flow.getVariableIfUsedOrReal(elvisExpression.lhs) ?: return@mergeIncomingFlow
            flow.commitOperationStatement(lhs notEq null)
        }
        rhsEnterNode.mergeIncomingFlow { _, flow ->
            val lhs = flow.getVariableIfUsedOrReal(elvisExpression.lhs) ?: return@mergeIncomingFlow
            flow.commitOperationStatement(lhs eq null)
        }
    }

    fun exitElvis(elvisExpression: FirElvisExpression, isLhsNotNull: Boolean, callCompleted: Boolean) {
        val node = graphBuilder.exitElvis(isLhsNotNull, callCompleted)
        node.mergeIncomingFlow { path, flow ->
            // If LHS is never null, then the edge from RHS is dead and this node's flow already contains
            // all statements from LHS unconditionally.
            if (isLhsNotNull) return@mergeIncomingFlow

            val elvisVariable = SyntheticVariable(elvisExpression)

            // If (x ?: null) != null then x != null
            @OptIn(UnresolvedExpressionTypeAccess::class) // Lambdas can have unresolved type here, see KT-61837
            if (elvisExpression.rhs.coneTypeOrNull?.isNullableNothing == true) {
                val lhsVariable = flow.getVariableIfUsedOrReal(elvisExpression.lhs)
                if (lhsVariable != null) {
                    flow.addImplication((elvisVariable notEq null) implies (lhsVariable notEq null))
                }
            }

            // If (null ?: x) != null then x != null
            @OptIn(UnresolvedExpressionTypeAccess::class) // Lambdas can have unresolved type here, see KT-61837
            if (elvisExpression.lhs.coneTypeOrNull?.isNullableNothing == true) {
                val rhsVariable = flow.getVariableIfUsedOrReal(elvisExpression.rhs)
                if (rhsVariable != null) {
                    flow.addImplication((elvisVariable notEq null) implies (rhsVariable notEq null))
                }
            }

            // For any predicate P(x), if P(v) != P(u ?: v) then u != null. In general this requires two levels of
            // implications, but for constant v the logic system can handle some basic cases of P(x).
            val rhs = (elvisExpression.rhs as? FirLiteralExpression)?.value as? Boolean
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

    // The data flow state from which type statements are taken during expression resolution.
    // Should normally be equal to `graphBuilder.lastNode`, but one exception is between exiting call
    // arguments and exiting the call itself, where smart casting does not use information from the arguments.
    var currentSmartCastPosition: Flow? = null
        private set

    private fun CFGNode<*>.buildDefaultFlow(
        builder: (FlowPath, MutableFlow) -> Unit,
    ): MutableFlow {
        val previousFlows = mutableListOf<PersistentFlow>()
        val statementFlows = mutableListOf<PersistentFlow>()

        for (node in previousNodes) {
            val edge = edgeFrom(node)
            if (!usedInDfa(edge)) continue

            // `MergePostponedLambdaExitsNode` nodes form a parallel data flow graph. We never compute
            // data flow for any of them until reaching a completed call.
            if (node is MergePostponedLambdaExitsNode && !node.flowInitialized) node.mergeIncomingFlow()

            // For CFGNodes that are the end of alternate flows, use the alternate flow associated with the edge label.
            val flow = if (node is FinallyBlockExitNode) {
                val alternatePath = FlowPath.CfgEdge(edge.label, node.fir)
                node.getAlternateFlow(alternatePath) ?: node.flow
            } else {
                node.flow
            }
            previousFlows.add(flow)
            if (edge.label != PostponedPath) {
                statementFlows.add(flow)
            }
        }

        val result = logicSystem.joinFlow(previousFlows, statementFlows, isUnion)

        if (graphBuilder.lastNodeOrNull == this) {
            if (currentSmartCastPosition == null || currentSmartCastPosition != previousFlows.singleOrNull()) {
                // Force-update the receiver stack as merging multiple flows might have changed receivers' type statements.
                resetSmartCastPositionTo(result)
            } else {
                // Receiver stack should already be up-to-date, only need to swap the flow for explicit lookups.
                currentSmartCastPosition = result
            }
        }

        builder(FlowPath.Default, result)
        return result
    }

    private fun CFGNode<*>.buildAlternateFlow(
        path: FlowPath.CfgEdge,
        builder: (FlowPath, MutableFlow) -> Unit,
    ): MutableFlow {
        val alternateFlowStart = this is FinallyBlockEnterNode
        val previousFlows = mutableListOf<PersistentFlow>()
        val statementFlows = mutableListOf<PersistentFlow>()

        for (node in previousNodes) {
            val edge = edgeFrom(node)
            if (!usedInDfa(edge)) continue

            // For CFGNodes that cause alternate flow paths to be created, only edges with matching labels should be merged. However, when
            // an alternate flow is being propagated through one of these CFGNodes - i.e., when the FirElements do not match - only
            // NormalPath edges should be merged.
            if (alternateFlowStart) {
                if (path.fir == this.fir && edge.label != path.label) {
                    continue
                } else if (path.fir != this.fir && edge.label != NormalPath) {
                    continue
                }
            }

            val flow = node.getAlternateFlow(path) ?: node.flow
            previousFlows.add(flow)
            if (edge.label != PostponedPath) {
                statementFlows.add(flow)
            }
        }

        val result = logicSystem.joinFlow(previousFlows, statementFlows, isUnion)
        builder(path, result)
        return result
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
        if (currentSmartCastPosition === mutableDefaultFlow) {
            currentSmartCastPosition = defaultFlow
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

    private fun ControlFlowGraph.completePostponedNodes() {
        for (subGraph in subGraphs) {
            subGraph.completePostponedNodes()
        }
        for (node in nodes) {
            if (node !is ClassExitNode && !node.flowInitialized) {
                node.mergeIncomingFlow()
            }
        }
    }

    // In rare cases (like after exiting functions) after adding more nodes `graphBuilder` will revert the current
    // state to a previously created node, so none of the nodes it returned are `lastNode` and `mergeIncomingFlow`
    // will not ensure the smart cast position is auto-advanced. In that case an explicit call to `resetSmartCastPosition`
    // is needed to roll back to that previously created node's state.
    fun resetSmartCastPosition() {
        resetSmartCastPositionTo(graphBuilder.lastNodeOrNull?.flow)
    }

    // This method can be used to change the smart cast state to some node that is not the one at which the graph
    // builder is currently stopped. This is temporary: adding any more nodes to the graph will restart tracking
    // of the current position in the graph.
    private fun resetSmartCastPositionTo(flow: Flow?) {
        val previous = currentSmartCastPosition
        if (previous == flow) return
        receiverStack.implicitValues.forEach {
            val variable = RealVariable.implicit(it.boundSymbol, it.originalType)
            val newStatement = flow?.getTypeStatement(variable)
            if (newStatement != previous?.getTypeStatement(variable)) {
                implicitUpdated(newStatement ?: MutableTypeStatement(variable))
            }
        }
        currentSmartCastPosition = flow
    }

    private fun isSameValueIn(other: PersistentFlow, fir: FirExpression, original: MutableFlow): Boolean {
        val variable = other.getRealVariableWithoutUnwrappingAlias(fir)
        return variable == null || logicSystem.isSameValueIn(other, original, variable)
    }

    private fun MutableFlow.addImplication(statement: Implication) {
        logicSystem.addImplication(this, statement)
    }

    private fun MutableFlow.addTypeStatement(info: TypeStatement) {
        val newStatement = logicSystem.addTypeStatement(this, info) ?: return
        val dataFlowVariable = newStatement.variable
        if (dataFlowVariable is RealVariable && dataFlowVariable.isImplicit && this === currentSmartCastPosition) {
            implicitUpdated(newStatement)
        }
    }

    private fun MutableFlow.addAllStatements(statements: TypeStatements) {
        statements.values.forEach { addTypeStatement(it) }
    }

    private fun MutableFlow.addAllConditionally(condition: OperationStatement, statements: TypeStatements) {
        statements.values.forEach { addImplication(condition implies it) }
    }

    private fun MutableFlow.addAllConditionally(condition: OperationStatement, from: Flow) {
        addAllConditionally(condition, getTypeStatementsNotInheritedFrom(from))
    }

    // Merging flow from two nodes can discard type statements. `mergedFlow.getTypeStatementsNotInheritedFrom(parentFlow)`
    // will produce the statements that were discarded (and maybe some that weren't).
    private fun MutableFlow.getTypeStatementsNotInheritedFrom(parent: Flow): TypeStatements =
        buildMap {
            parent.knownVariables.forEach {
                if (unwrapVariable(it) != it) return@forEach // will add a statement for the aliased variable instead
                val statement = parent.getTypeStatement(it)
                if (statement != null && statement != getTypeStatement(it)) put(it, statement)
            }
        }

    private fun MutableFlow.commitOperationStatement(statement: OperationStatement) {
        addAllStatements(logicSystem.approveOperationStatement(this, statement, removeApprovedOrImpossible = true))
    }

    private fun Flow.getVariable(fir: FirExpression, createReal: Boolean): DataFlowVariable? =
        variableStorage.get(fir, createReal, unwrapAlias = { unwrapVariableIfStable(it) })

    fun Flow.getVariableWithoutUnwrappingAlias(fir: FirExpression, createReal: Boolean): DataFlowVariable? =
        variableStorage.get(fir, createReal, unwrapAlias = { it }, unwrapAliasInReceivers = { unwrapVariableIfStable(it) })

    // Use this when making non-type statements, such as `variable eq true`.
    // Returns null if the statement would be useless (the variable has not been used in any implications).
    private fun Flow.getVariableIfUsed(fir: FirExpression): DataFlowVariable? =
        getVariable(fir, createReal = false)?.takeIf { !getImplications(it).isNullOrEmpty() }

    // Use this when making type statements, such as `variable typeEq ...` or `variable notEq null`.
    // Returns null if the statement would be useless (the variable is synthetic and has not been used in any implications).
    private fun Flow.getVariableIfUsedOrReal(fir: FirExpression): DataFlowVariable? =
        getVariable(fir, createReal = true)?.takeIf { it.isReal() || !getImplications(it).isNullOrEmpty() }

    // Use this for variables on the left side of an implication if `fir` could be a variable access. Most statements
    // that create implications are not variable accesses, in which case `SyntheticVariable` can be created directly.
    // Returns null only if the variable is an unstable alias, and so cannot be used at all.
    private fun Flow.getOrCreateVariable(fir: FirExpression): DataFlowVariable? =
        getVariable(fir, createReal = true)

    fun getOrCreateVariable(fir: FirExpression): DataFlowVariable? {
        return currentSmartCastPosition?.getOrCreateVariable(fir)
    }

    // Use this for calling `getTypeStatement` or accessing reassignment information.
    // Returns null if it's already known that no statements about the variable were made ever.
    private fun Flow.getRealVariableWithoutUnwrappingAlias(fir: FirExpression): RealVariable? =
        getVariableWithoutUnwrappingAlias(fir, createReal = false) as? RealVariable

    private fun Flow.unwrapVariableIfStable(variable: RealVariable): RealVariable? =
        unwrapVariable(variable).takeIf { it == variable || !variable.isUnstableLocalVar(types = null) }

    private fun getLocal(symbol: FirPropertySymbol, create: Boolean): RealVariable? {
        // In the REPL, "local" variables are actually REPL-snippet class-level properties.
        // These properties will have a dispatcher receiver, so they need to be created with a dispatch variable.
        val dispatchReceiver = symbol.dispatchReceiverType?.let { type ->
            val prototype = RealVariable(
                symbol = type.toSymbol() ?: return null,
                isImplicit = true,
                dispatchReceiver = null,
                extensionReceiver = null,
                originalType = type,
            )
            val actual = if (create) prototype else variableStorage.getKnown(prototype)
            actual ?: return null
        }

        val prototype = RealVariable(
            symbol = symbol,
            isImplicit = false,
            dispatchReceiver = dispatchReceiver,
            extensionReceiver = null,
            originalType = symbol.resolvedReturnType
        )
        return if (create) variableStorage.remember(prototype) else variableStorage.getKnown(prototype)
    }
}
