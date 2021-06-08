/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.cfa

import org.jetbrains.kotlin.contracts.description.EventOccurrencesRange
import org.jetbrains.kotlin.contracts.description.canBeRevisited
import org.jetbrains.kotlin.contracts.description.isDefinitelyVisited
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.analysis.diagnostics.reportOn
import org.jetbrains.kotlin.fir.declarations.isLateInit
import org.jetbrains.kotlin.fir.declarations.referredPropertySymbol
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccess
import org.jetbrains.kotlin.fir.expressions.FirVariableAssignment
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.*
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirVariableSymbol

object FirPropertyInitializationAnalyzer : AbstractFirPropertyInitializationChecker() {
    override fun analyze(
        graph: ControlFlowGraph,
        reporter: DiagnosticReporter,
        data: Map<CFGNode<*>, PathAwarePropertyInitializationInfo>,
        properties: Set<FirPropertySymbol>,
        capturedWrites: Set<FirVariableAssignment>,
        context: CheckerContext
    ) {
        val localData = data.filter {
            val symbolFir = (it.key.fir as? FirVariableSymbol<*>)?.fir
            symbolFir == null || symbolFir.initializer == null && symbolFir.delegate == null
        }

        val localProperties = properties.filter { it.fir.initializer == null && it.fir.delegate == null }.toSet()

        val reporterVisitor = PropertyReporter(localData, localProperties, capturedWrites, reporter, context)
        graph.traverse(TraverseDirection.Forward, reporterVisitor)
    }

    private class PropertyReporter(
        val data: Map<CFGNode<*>, PathAwarePropertyInitializationInfo>,
        val localProperties: Set<FirPropertySymbol>,
        val capturedWrites: Set<FirVariableAssignment>,
        val reporter: DiagnosticReporter,
        val context: CheckerContext
    ) : ControlFlowGraphVisitorVoid() {
        override fun visitNode(node: CFGNode<*>) {}

        private fun getPropertySymbol(node: CFGNode<*>): FirPropertySymbol? {
            return (node.fir as? FirQualifiedAccess)?.referredPropertySymbol
        }

        override fun visitVariableAssignmentNode(node: VariableAssignmentNode) {
            val symbol = getPropertySymbol(node) ?: return
            val pathAwareInfo = data.getValue(node)
            for (label in pathAwareInfo.keys) {
                if (investigateVariableAssignment(pathAwareInfo[label]!!, symbol, node)) {
                    // To avoid duplicate reports, stop investigating remaining paths if the property is re-initialized at any path.
                    break
                }
            }
        }

        private fun investigateVariableAssignment(
            info: PropertyInitializationInfo,
            symbol: FirPropertySymbol,
            node: VariableAssignmentNode
        ): Boolean {
            if (symbol.fir.isVal && node.fir in capturedWrites) {
                if (symbol.fir.isLocal) {
                    reporter.reportOn(node.fir.lValue.source, FirErrors.CAPTURED_VAL_INITIALIZATION, symbol, context)
                } else {
                    reporter.reportOn(node.fir.lValue.source, FirErrors.CAPTURED_MEMBER_VAL_INITIALIZATION, symbol, context)
                }
                return true
            }
            val kind = info[symbol] ?: EventOccurrencesRange.ZERO
            if (symbol.fir.isVal && kind.canBeRevisited()) {
                reporter.reportOn(node.fir.lValue.source, FirErrors.VAL_REASSIGNMENT, symbol, context)
                return true
            }
            return false
        }

        override fun visitQualifiedAccessNode(node: QualifiedAccessNode) {
            val reference = node.fir.calleeReference as? FirResolvedNamedReference ?: return
            val symbol = reference.resolvedSymbol as? FirPropertySymbol ?: return
            if (symbol !in localProperties) return
            if (symbol.fir.isLateInit) return
            val pathAwareInfo = data.getValue(node)
            for (info in pathAwareInfo.values) {
                if (investigateVariableAccess(info, symbol, node)) {
                    // To avoid duplicate reports, stop investigating remaining paths if the property is not initialized at any path.
                    break
                }
            }
        }

        private fun investigateVariableAccess(
            info: PropertyInitializationInfo,
            symbol: FirPropertySymbol,
            node: QualifiedAccessNode
        ): Boolean {
            val kind = info[symbol] ?: EventOccurrencesRange.ZERO
            if (!kind.isDefinitelyVisited()) {
                reporter.reportOn(node.fir.source, FirErrors.UNINITIALIZED_VARIABLE, symbol, context)
                return true
            }
            return false
        }
    }
}
