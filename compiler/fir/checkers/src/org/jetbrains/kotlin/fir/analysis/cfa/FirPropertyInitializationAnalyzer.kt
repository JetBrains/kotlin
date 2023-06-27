/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.cfa

import org.jetbrains.kotlin.contracts.description.canBeRevisited
import org.jetbrains.kotlin.contracts.description.isDefinitelyVisited
import org.jetbrains.kotlin.contracts.description.isInPlace
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.cfa.util.PropertyInitializationInfoData
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.hasBackingField
import org.jetbrains.kotlin.fir.declarations.utils.hasExplicitBackingField
import org.jetbrains.kotlin.fir.declarations.utils.isLateInit
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.isCatchParameter
import org.jetbrains.kotlin.fir.references.toResolvedPropertySymbol
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.*
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.ControlFlowGraph.Kind
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirSyntheticPropertySymbol

object FirPropertyInitializationAnalyzer : AbstractFirPropertyInitializationChecker() {
    override fun analyze(data: PropertyInitializationInfoData, reporter: DiagnosticReporter, context: CheckerContext) {
        data.checkPropertyAccesses(isForClassInitialization = false, context, reporter)
    }
}

val FirDeclaration.evaluatedInPlace: Boolean
    get() = when (this) {
        is FirAnonymousFunction -> invocationKind.isInPlace
        is FirAnonymousObject -> classKind != ClassKind.ENUM_ENTRY
        is FirConstructor -> true // child of class initialization graph
        is FirFunction, is FirClass -> false
        else -> true // property initializer, etc.
    }

/**
 * [isForClassInitialization] means that caller is interested in member property in the scope
 *   of class initialization section. In this case the fact that property has initializer does
 *   not mean that it's safe to access this property in any place:
 *
 * class A {
 *     val b = a // a is not initialized here
 *     val a = 10
 *     val c = a // but initialized here
 * }
 */

@OptIn(SymbolInternals::class)
fun FirPropertySymbol.requiresInitialization(isForClassInitialization: Boolean): Boolean {
    val hasImplicitBackingField = !hasExplicitBackingField && hasBackingField
    return when {
        this is FirSyntheticPropertySymbol -> false
        isForClassInitialization -> hasDelegate || hasImplicitBackingField
        else -> !hasInitializer && hasImplicitBackingField && fir.isCatchParameter != true
    }
}

fun PropertyInitializationInfoData.checkPropertyAccesses(
    isForClassInitialization: Boolean,
    context: CheckerContext,
    reporter: DiagnosticReporter
) {
    // If a property has an initializer (or does not need one), then any reads are OK while any writes are OK
    // if it's a `var` and bad if it's a `val`. `FirReassignmentAndInvisibleSetterChecker` does this without a CFG.
    val filtered = properties.filterTo(mutableSetOf()) { it.requiresInitialization(isForClassInitialization) }
    if (filtered.isEmpty()) return

    checkPropertyAccesses(
        graph, filtered, context, reporter, scope = null,
        isForClassInitialization, doNotReportUninitializedVariable = false, mutableMapOf()
    )
}

@OptIn(SymbolInternals::class)
private fun PropertyInitializationInfoData.checkPropertyAccesses(
    graph: ControlFlowGraph,
    properties: Set<FirPropertySymbol>,
    context: CheckerContext,
    reporter: DiagnosticReporter,
    scope: FirDeclaration?,
    isForClassInitialization: Boolean,
    doNotReportUninitializedVariable: Boolean,
    scopes: MutableMap<FirPropertySymbol, FirDeclaration?>
) {
    fun FirQualifiedAccessExpression.hasCorrectReceiver() =
        (dispatchReceiver.unwrapSmartcastExpression() as? FirThisReceiverExpression)?.calleeReference?.boundSymbol == receiver

    for (node in graph.nodes) {
        when {
            // TODO, KT-59669: `node.isUnion` - f({ x = 1 }, { x = 2 }) - which to report?
            //  Also this is currently indistinguishable from x = 1; f({}, {}).

            node is VariableDeclarationNode -> {
                val symbol = node.fir.symbol
                if (scope != null && receiver == null && node.fir.isVal && symbol in properties) {
                    // It's OK to initialize this variable from a nested called-in-place function, but not from
                    // a non-called-in-place function or a non-anonymous-object class initializer.
                    scopes[symbol] = scope
                }
            }

            node is VariableAssignmentNode -> {
                val symbol = node.fir.calleeReference?.toResolvedPropertySymbol() ?: continue
                if (!symbol.fir.isVal || node.fir.unwrapLValue()?.hasCorrectReceiver() != true || symbol !in properties) continue

                if (getValue(node).values.any { it[symbol]?.canBeRevisited() == true }) {
                    reporter.reportOn(node.fir.lValue.source, FirErrors.VAL_REASSIGNMENT, symbol, context)
                } else if (scope != scopes[symbol]) {
                    val error = if (receiver != null)
                        FirErrors.CAPTURED_MEMBER_VAL_INITIALIZATION
                    else
                        FirErrors.CAPTURED_VAL_INITIALIZATION
                    reporter.reportOn(node.fir.lValue.source, error, symbol, context)
                }
            }

            node is QualifiedAccessNode -> {
                if (doNotReportUninitializedVariable) continue
                val symbol = node.fir.calleeReference.toResolvedPropertySymbol() ?: continue
                if (!symbol.isLateInit && node.fir.hasCorrectReceiver() && symbol in properties &&
                    getValue(node).values.any { it[symbol]?.isDefinitelyVisited() != true }
                ) {
                    reporter.reportOn(node.fir.source, FirErrors.UNINITIALIZED_VARIABLE, symbol, context)
                }
            }

            // In the class case, subgraphs of the exit node are member functions, which are considered to not
            // be part of initialization, so any val is considered to be initialized there and the CFG is not
            // needed. The errors on reassignments will be emitted by `FirReassignmentAndInvisibleSetterChecker`.
            node is CFGNodeWithSubgraphs<*> && (receiver == null || node !== graph.exitNode) -> {
                for (subGraph in node.subGraphs) {
                    /*
                     * For class initialization graph we allow to read properties in non-in-place lambdas
                     *   even if they may be not initialized at this point, because if lambda is not in-place,
                     *   then it most likely will be called after object will be initialized
                     */
                    val doNotReportForSubGraph = doNotReportUninitializedVariable ||
                            (isForClassInitialization && subGraph.kind.doNotReportUninitializedVariableForClassInitialization)

                    val newScope = subGraph.declaration?.takeIf { !it.evaluatedInPlace } ?: scope
                    checkPropertyAccesses(
                        subGraph, properties, context, reporter, newScope,
                        isForClassInitialization, doNotReportForSubGraph, scopes
                    )
                }
            }
        }
    }
}

private val Kind.doNotReportUninitializedVariableForClassInitialization: Boolean
    get() = when (this) {
        Kind.AnonymousFunction, Kind.LocalFunction -> true
        else -> false
    }
