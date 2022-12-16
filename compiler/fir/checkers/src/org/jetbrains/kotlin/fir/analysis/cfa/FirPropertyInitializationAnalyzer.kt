/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.cfa

import org.jetbrains.kotlin.contracts.description.canBeRevisited
import org.jetbrains.kotlin.contracts.description.isDefinitelyVisited
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.cfa.util.PropertyInitializationInfoData
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.utils.isLateInit
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccess
import org.jetbrains.kotlin.fir.expressions.FirVariableAssignment
import org.jetbrains.kotlin.fir.isCatchParameter
import org.jetbrains.kotlin.fir.references.toResolvedPropertySymbol
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.*
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirSyntheticPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirVariableSymbol

@OptIn(SymbolInternals::class)
object FirPropertyInitializationAnalyzer : AbstractFirPropertyInitializationChecker() {
    override fun analyze(
        graph: ControlFlowGraph,
        reporter: DiagnosticReporter,
        data: PropertyInitializationInfoData,
        properties: Set<FirPropertySymbol>,
        capturedWrites: Set<FirVariableAssignment>,
        context: CheckerContext
    ) {
        val localProperties = properties.filterNotTo(mutableSetOf()) { it.isInitialized() }
        val reporterVisitor = PropertyReporter(data, localProperties, capturedWrites, reporter, context)
        graph.traverse(reporterVisitor)
    }

    private fun FirVariableSymbol<*>.isInitialized(): Boolean {
        return fir.initializer != null
                || fir.delegate != null
                || this is FirPropertySymbol && fir.isCatchParameter == true
    }

    private class PropertyReporter(
        val data: PropertyInitializationInfoData,
        val localProperties: Set<FirPropertySymbol>,
        val capturedWrites: Set<FirVariableAssignment>,
        val reporter: DiagnosticReporter,
        val context: CheckerContext
    ) : ControlFlowGraphVisitorVoid() {
        override fun visitNode(node: CFGNode<*>) {}

        override fun <T> visitUnionNode(node: T) where T : CFGNode<*>, T : UnionNodeMarker {
            // TODO: f({ x = 1 }, { x = 2 }) - which to report?
            //  Also this is currently indistinguishable from x = 1; f({}, {}).
        }

        private val CFGNode<*>.propertySymbol: FirPropertySymbol?
            get() = (fir as? FirQualifiedAccess)?.calleeReference?.toResolvedPropertySymbol()

        override fun visitVariableAssignmentNode(node: VariableAssignmentNode) {
            val symbol = node.propertySymbol ?: return
            if (!symbol.fir.isVal) return

            if (node.fir in capturedWrites) {
                if (symbol.fir.isLocal) {
                    reporter.reportOn(node.fir.lValue.source, FirErrors.CAPTURED_VAL_INITIALIZATION, symbol, context)
                } else {
                    reporter.reportOn(node.fir.lValue.source, FirErrors.CAPTURED_MEMBER_VAL_INITIALIZATION, symbol, context)
                }
            } else if (data.getValue(node).values.any { it[symbol]?.canBeRevisited() == true }) {
                reporter.reportOn(node.fir.lValue.source, FirErrors.VAL_REASSIGNMENT, symbol, context)
            }
        }

        override fun visitQualifiedAccessNode(node: QualifiedAccessNode) {
            val symbol = node.propertySymbol ?: return
            if (symbol in localProperties && !symbol.fir.isLateInit && symbol !is FirSyntheticPropertySymbol &&
                !data.getValue(node).values.all { it[symbol]?.isDefinitelyVisited() == true }
            ) {
                reporter.reportOn(node.fir.source, FirErrors.UNINITIALIZED_VARIABLE, symbol, context)
            }
        }
    }
}
