/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.cfa

import org.jetbrains.kotlin.contracts.description.EventOccurrencesRange
import org.jetbrains.kotlin.contracts.description.canBeRevisited
import org.jetbrains.kotlin.contracts.description.isDefinitelyVisited
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.getContainingClass
import org.jetbrains.kotlin.fir.analysis.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.analysis.diagnostics.reportOn
import org.jetbrains.kotlin.fir.declarations.utils.isLateInit
import org.jetbrains.kotlin.fir.declarations.utils.referredPropertySymbol
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccess
import org.jetbrains.kotlin.fir.expressions.FirVariableAssignment
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
        val properties: Set<FirPropertySymbol>,
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
            // We're interested in `val` property only.
            if (!symbol.fir.isVal) return false

            if (node.fir in capturedWrites) {
                if (symbol.fir.isLocal) {
                    reporter.reportOn(node.fir.source, FirErrors.CAPTURED_VAL_INITIALIZATION, symbol, context)
                } else {
                    reporter.reportOn(node.fir.source, FirErrors.CAPTURED_MEMBER_VAL_INITIALIZATION, symbol, context)
                }
                return true
            }

            // ------ Member properties

            if (!symbol.fir.isLocal) {
                // Member `val` property can be initialized in constructors, anonymous initializers, or initializers of other properties.
                // Otherwise, this is a forbidden `val` assignment.
                var assignmentContextAllowedForMemberProperty: FirDeclaration? = null
                var cfg: ControlFlowGraph? = node.owner
                while (assignmentContextAllowedForMemberProperty == null && cfg != null) {
                    assignmentContextAllowedForMemberProperty = when (val declaration = cfg.declaration) {
                        is FirAnonymousInitializer,
                        is FirConstructor,
                        is FirProperty -> declaration
                        else -> null
                    }
                    cfg = cfg.owner
                }
                // Double-check if the assignment context belongs to the containing class of the property of interest.
                if (assignmentContextAllowedForMemberProperty != null) {
                    val propertyContainingClass = symbol.fir.getContainingClass(context) as? FirClass
                    if (propertyContainingClass?.declarations?.contains(assignmentContextAllowedForMemberProperty) == false) {
                        // Otherwise, that is a nested one, e.g.,
                        // ```
                        //   init {
                        //     prop = object {
                        //       init {
                        //         p = v
                        //       }
                        //     }
                        //   }
                        // ```
                        assignmentContextAllowedForMemberProperty = null
                    }
                }
                if (assignmentContextAllowedForMemberProperty == null &&
                    context.propertyInitializationInfoCache.isInitializedMemberProperty(symbol)
                ) {
                    reporter.reportOn(node.fir.source, FirErrors.VAL_REASSIGNMENT, symbol, context)
                    return true
                }

            }

            // ------ Local properties in general
            // Or member properties in allowed assignment context

            val kind = info[symbol] ?: EventOccurrencesRange.ZERO
            if (kind.canBeRevisited()) {
                reporter.reportOn(node.fir.source, FirErrors.VAL_REASSIGNMENT, symbol, context)
                return true
            }
            return false
        }

        override fun visitQualifiedAccessNode(node: QualifiedAccessNode) {
            val symbol = getPropertySymbol(node) ?: return
            if (symbol !in properties) return
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
