/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.cfa

import kotlinx.collections.immutable.PersistentSet
import kotlinx.collections.immutable.persistentSetOf
import org.jetbrains.kotlin.KtRealSourceElementKind
import org.jetbrains.kotlin.contracts.description.canBeRevisited
import org.jetbrains.kotlin.contracts.description.isDefinitelyVisited
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.cfa.util.VariableInitializationInfoData
import org.jetbrains.kotlin.fir.analysis.cfa.util.previousCfgNodes
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.getContainingSymbol
import org.jetbrains.kotlin.fir.analysis.checkers.hasDiagnosticKind
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.isConst
import org.jetbrains.kotlin.fir.declarations.utils.isExternal
import org.jetbrains.kotlin.fir.declarations.utils.isLateInit
import org.jetbrains.kotlin.fir.diagnostics.DiagnosticKind
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.packageFqName
import org.jetbrains.kotlin.fir.references.toResolvedVariableSymbol
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.*
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.ControlFlowGraph.Kind
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirVariableSymbol
import org.jetbrains.kotlin.fir.types.resolvedType
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

abstract class VariableInitializationCheckProcessor {
    fun check(
        data: VariableInitializationInfoData,
        isForInitialization: Boolean,
        context: CheckerContext,
        reporter: DiagnosticReporter
    ) {
        val filtered = filterProperties(data, isForInitialization)
        if (filtered.isEmpty()) return

        data.runCheck(
            data.graph, filtered, context, reporter, scope = null,
            isForInitialization,
            doNotReportUninitializedVariable = false,
            doNotReportConstantUninitialized = true,
            scopes = mutableMapOf(),
        )
    }

    // TODO: move this to PropertyInitializationInfoData (the collector also does this check when visiting assignments)
    private fun VariableInitializationInfoData.runCheck(
        graph: ControlFlowGraph,
        properties: Set<FirVariableSymbol<*>>,
        context: CheckerContext,
        reporter: DiagnosticReporter,
        scope: FirDeclaration?,
        isForInitialization: Boolean,
        doNotReportUninitializedVariable: Boolean,
        doNotReportConstantUninitialized: Boolean,
        scopes: MutableMap<FirVariableSymbol<*>, FirDeclaration?>,
    ) {
        for (node in graph.nodes) {
            if (node.isUnion) {
                processUnionNode(node, properties, context, reporter)
            }

            when (node) {
                is VariableDeclarationNode -> processVariableDeclaration(node, scope, properties, scopes)
                is VariableAssignmentNode -> processVariableAssignment(node, properties, reporter, context, scope, scopes)
                is QualifiedAccessNode -> processQualifiedAccess(
                    node, node.fir, properties,
                    doNotReportUninitializedVariable,
                    doNotReportConstantUninitialized,
                    reporter, context
                )
                is FunctionCallEnterNode -> {
                    val call = node.fir
                    if (call is FirImplicitInvokeCall) {
                        // TODO(KT-76534) CFG should have a QualifiedAccessNode for the implicit receiver
                        val receiverExitNode = (node.firstPreviousNode as? FunctionCallArgumentsExitNode)?.explicitReceiverExitNode
                        val receiver = (call.dispatchReceiver ?: call.explicitReceiver) as? FirQualifiedAccessExpression
                        if (receiverExitNode != null && receiver != null) {
                            processQualifiedAccess(
                                receiverExitNode, receiver, properties,
                                doNotReportUninitializedVariable,
                                doNotReportConstantUninitialized,
                                reporter, context
                            )
                        }
                    }
                }
                is CFGNodeWithSubgraphs<*> -> {
                    processSubGraphs(
                        graph, node, properties, context, reporter,
                        scope, isForInitialization,
                        doNotReportUninitializedVariable,
                        doNotReportConstantUninitialized,
                        scopes
                    )
                }
                else -> {}
            }
        }
    }

    private fun VariableInitializationInfoData.processUnionNode(
        node: CFGNode<*>,
        properties: Set<FirVariableSymbol<*>>,
        context: CheckerContext,
        reporter: DiagnosticReporter,
    ) {
        fun CFGNode<*>.reportErrorsOnInitializationsInInputs(
            symbol: FirVariableSymbol<*>,
            path: EdgeLabel,
            visited: PersistentSet<CFGNode<*>>,
        ) {
            val newVisited = visited.add(this)
            require(newVisited !== visited) { buildRecursionErrorMessage(this, symbol, context) }

            for (previousNode in previousCfgNodes) {
                if (edgeFrom(previousNode).kind.isBack) continue
                when (val assignmentNode = getValue(previousNode)[path]?.get(symbol)?.location) {
                    is VariableDeclarationNode -> {} // unreachable - `val`s with initializers do not require hindsight
                    is VariableAssignmentNode -> reportCapturedInitialization(assignmentNode, symbol, reporter, context)
                    else -> // merge node for a branching construct, e.g. `if (p) { x = 1 } else { x = 2 }` - report on all branches
                        assignmentNode?.reportErrorsOnInitializationsInInputs(symbol, path, newVisited)
                }
            }
        }

        for ((path, data) in getValue(node)) {
            if (path == CapturedByValue) continue // CaptureByValue path does not contain enough information for captured initialization checks.

            for ((symbol, range) in data) {
                if (!symbol.isVal || !range.canBeRevisited() || symbol !in properties) continue
                // This can be something like `f({ x = 1 }, { x = 2 })` where `f` calls both lambdas in-place.
                // At each assignment it was only considered in isolation, but now that we're merging their control flows,
                // we can see that the assignments clash, so we need to go back and emit errors on these nodes.
                if (node.previousCfgNodes.all { getValue(it)[path]?.get(symbol)?.canBeRevisited() != true }) {
                    node.reportErrorsOnInitializationsInInputs(symbol, path, visited = persistentSetOf())
                }
            }
        }
    }

    private fun VariableInitializationInfoData.processVariableDeclaration(
        node: VariableDeclarationNode,
        scope: FirDeclaration?,
        properties: Set<FirVariableSymbol<*>>,
        scopes: MutableMap<FirVariableSymbol<*>, FirDeclaration?>,
    ) {
        val symbol = node.fir.symbol
        if (scope != null && receiver == null && node.fir.isVal && symbol in properties) {
            // It's OK to initialize this variable from a nested called-in-place function, but not from
            // a non-called-in-place function or a non-anonymous-object class initializer.
            scopes[symbol] = scope
        }
    }

    private fun VariableInitializationInfoData.processVariableAssignment(
        node: VariableAssignmentNode,
        properties: Set<FirVariableSymbol<*>>,
        reporter: DiagnosticReporter,
        context: CheckerContext,
        scope: FirDeclaration?,
        scopes: MutableMap<FirVariableSymbol<*>, FirDeclaration?>,
    ) {
        val symbol = node.fir.calleeReference?.toResolvedVariableSymbol() ?: return
        if (!symbol.isVal || node.fir.unwrapLValue()?.hasMatchingReceiver(this) != true || symbol !in properties) return

        val info = getValue(node)
        when {
            info.values.any { it[symbol]?.canBeRevisited() == true } -> {
                reportValReassignment(node, symbol, reporter, context)
            }
            scope != scopes[symbol] -> {
                reportCapturedInitialization(node, symbol, reporter, context)
            }
            !symbol.isLocal && !node.owner.isInline(until = symbol.getContainingSymbol(context.session)) -> {
                // If the assignment is inside INVOKE_ONCE lambda and the lambda is not inlined,
                // backend generates either separate function or separate class for the lambda.
                // If we try to initialize non-static final field there, we will get exception at
                // runtime, since we can initialize such fields only inside constructors.
                reportNonInlineMemberValInitialization(node, symbol, reporter, context)
            }
        }
    }

    private fun VariableInitializationInfoData.processQualifiedAccess(
        node: CFGNode<*>,
        expression: FirQualifiedAccessExpression,
        properties: Set<FirVariableSymbol<*>>,
        doNotReportUninitializedVariable: Boolean,
        doNotReportConstantUninitialized: Boolean,
        reporter: DiagnosticReporter,
        context: CheckerContext,
    ) {
        if (doNotReportUninitializedVariable) return
        if (expression is FirWhenSubjectExpression) return
        if (expression.resolvedType.hasDiagnosticKind(DiagnosticKind.RecursionInImplicitTypes)) return
        val symbol = expression.calleeReference.toResolvedVariableSymbol() ?: return
        if (doNotReportConstantUninitialized && symbol.isConst) return
        if (symbol.source?.kind != KtRealSourceElementKind) return
        if (
            !symbol.isLateInit &&
            !symbol.isExternal &&
            expression.hasMatchingReceiver(this) &&
            symbol in properties &&
            !symbol.isInitializedAt(node, data = this)
        ) {
            reportUninitializedVariable(reporter, expression, symbol, context)
        }
    }

    private fun FirVariableSymbol<*>.isInitializedAt(node: CFGNode<*>, data: VariableInitializationInfoData): Boolean {
        return data.getValue(node).all { (key, value) ->
            (key == CapturedByValue && !isCapturedByValue) || value[this]?.isDefinitelyVisited() == true
        }
    }

    private fun VariableInitializationInfoData.processSubGraphs(
        graph: ControlFlowGraph,
        node: CFGNodeWithSubgraphs<*>,
        properties: Set<FirVariableSymbol<*>>,
        context: CheckerContext,
        reporter: DiagnosticReporter,
        scope: FirDeclaration?,
        isForInitialization: Boolean,
        doNotReportUninitializedVariable: Boolean,
        doNotReportConstantUninitialized: Boolean,
        scopes: MutableMap<FirVariableSymbol<*>, FirDeclaration?>,
    ) {
        // In the class case, subgraphs of the exit node are member functions, which are considered to not
        // be part of initialization, so any val is considered to be initialized there and the CFG is not
        // needed. The errors on reassignments will be emitted by `FirReassignmentAndInvisibleSetterChecker`.
        if (receiver != null && node === graph.exitNode) return
        for (subGraph in node.subGraphs) {
            /*
                         * For class initialization graph we allow to read properties in non-in-place lambdas
                         *   even if they may be not initialized at this point, because if lambda is not in-place,
                         *   then it most likely will be called after object will be initialized
                         */
            val doNotReportForSubGraph = isForInitialization && subGraph.kind.doNotReportUninitializedVariableForInitialization

            // Must report uninitialized variable if we start initializing a constant property. This
            // allows "regular" properties to reference constant properties out-of-order, but all other
            // property references must be in-order.
            val isSubGraphConstProperty = (subGraph.declaration as? FirProperty)?.isConst == true

            val newScope = subGraph.declaration?.takeIf { !it.evaluatedInPlace } ?: scope
            runCheck(
                subGraph, properties, context, reporter, newScope,
                isForInitialization,
                doNotReportUninitializedVariable = doNotReportUninitializedVariable || doNotReportForSubGraph,
                doNotReportConstantUninitialized = doNotReportConstantUninitialized && !isSubGraphConstProperty,
                scopes
            )
        }
    }

    // ------------------------------------ reporting ------------------------------------

    protected abstract fun VariableInitializationInfoData.reportCapturedInitialization(
        node: VariableAssignmentNode,
        symbol: FirVariableSymbol<*>,
        reporter: DiagnosticReporter,
        context: CheckerContext
    )

    protected abstract fun reportUninitializedVariable(
        reporter: DiagnosticReporter,
        expression: FirQualifiedAccessExpression,
        symbol: FirVariableSymbol<*>,
        context: CheckerContext,
    )

    protected abstract fun reportNonInlineMemberValInitialization(
        node: VariableAssignmentNode,
        symbol: FirVariableSymbol<*>,
        reporter: DiagnosticReporter,
        context: CheckerContext,
    )

    protected abstract fun reportValReassignment(
        node: VariableAssignmentNode,
        symbol: FirVariableSymbol<*>,
        reporter: DiagnosticReporter,
        context: CheckerContext,
    )

    // ------------------------------------ utilities ------------------------------------

    protected abstract fun filterProperties(
        data: VariableInitializationInfoData,
        isForInitialization: Boolean
    ): Set<FirVariableSymbol<*>>

    protected abstract fun FirQualifiedAccessExpression.hasMatchingReceiver(data: VariableInitializationInfoData): Boolean
}

private val Kind.doNotReportUninitializedVariableForInitialization: Boolean
    get() = when (this) {
        Kind.Function, Kind.AnonymousFunction, Kind.LocalFunction -> true
        else -> false
    }

/**
 * Determine if this declaration is evaluated inline. This is distinct from [evaluatedInPlace], as the declaration must also be inlined by
 * the compiler.
 */
private val FirDeclaration.evaluatedInline: Boolean
    get() = when (this) {
        is FirAnonymousFunction -> inlineStatus == InlineStatus.Inline
        is FirConstructor -> true // child of class initialization graph
        is FirFunction, is FirClass -> false
        else -> true // property initializer, etc.
    }

/**
 * Checks that [ControlFlowGraph.declaration] is [evaluatedInline], and also recursively check all
 * parent [ControlFlowGraph]s.
 *
 * @param until will stop recursion if [ControlFlowGraph.declaration] matches the specified symbol.
 * This is used to stop recursion when there are nested declarations (like a local class), and we
 * only need to check until that nested declaration.
 */
private fun ControlFlowGraph.isInline(until: FirBasedSymbol<*>?): Boolean {
    val declaration = declaration
    if (declaration?.symbol == until) return true
    if (declaration?.evaluatedInline != true) return false
    return enterNode.previousNodes.all { it.owner.isInline(until) }
}

private val FirVariableSymbol<*>.isLocal: Boolean
    get() = when (this) {
        is FirPropertySymbol -> isLocal
        else -> false
    }

val FirVariableSymbol<*>.isCapturedByValue: Boolean
    get() = isVal && isLocal

fun buildRecursionErrorMessage(
    problemNode: CFGNode<*>,
    symbol: FirVariableSymbol<*>,
    context: CheckerContext,
): String {
    return buildString {
        appendLine("Node has already been visited and could result in infinite recursion.")
        appendLine()
        append("File Path: ").appendLine(context.containingFilePath)
        append("Variable: ").appendLine(symbol.getDebugFqName())
        appendLine("Declarations:")
        problemNode.firstGraphDeclaration()?.let { declaration ->
            append("- ").append(declaration.symbol.getDebugFqName()).appendLine(" (graph declaration)")
        }
        for (declaration in context.containingDeclarations) {
            append("- ").appendLine(declaration.getDebugFqName())
        }
    }
}

private fun CFGNode<*>.firstGraphDeclaration(): FirDeclaration? {
    owner.declaration?.let { return it }
    return owner.enterNode.previousNodes.firstNotNullOfOrNull { it.firstGraphDeclaration() }
}

@OptIn(SymbolInternals::class)
private fun FirBasedSymbol<*>.getDebugFqName(): FqName {
    return when (val fir = this.fir) {
        is FirFile -> fir.packageFqName.child(Name.identifier(fir.name))
        is FirScript -> fir.symbol.fqName
        is FirReplSnippet -> FqName.topLevel(fir.name)
        is FirClassLikeDeclaration -> fir.symbol.classId.asSingleFqName()
        is FirTypeParameter -> fir.containingDeclarationSymbol.getDebugFqName().child(fir.name)
        is FirAnonymousInitializer -> fir.containingDeclarationSymbol.getDebugFqName().child(Name.special("<init>"))
        is FirCallableDeclaration -> fir.symbol.callableId.asFqNameForDebugInfo()
        is FirCodeFragment -> FqName.topLevel(Name.special("<fragment>"))
        is FirDanglingModifierList -> FqName.topLevel(Name.special("<dangling>"))
        is FirReceiverParameter -> FqName.topLevel(Name.special("<extension-receiver-parameter>"))
    }
}
