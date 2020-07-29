/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.cfa

import org.jetbrains.kotlin.contracts.description.EventOccurrencesRange
import org.jetbrains.kotlin.contracts.description.isDefinitelyVisited
import org.jetbrains.kotlin.fir.analysis.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.CFGNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.ControlFlowGraph
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.ControlFlowGraphVisitorVoid
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.QualifiedAccessNode
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol

object FirPropertyInitializationAnalyzer : AbstractFirPropertyInitializationChecker() {
    override fun analyze(graph: ControlFlowGraph, reporter: DiagnosticReporter) {
        val localProperties = LocalPropertyCollector.collect(graph)
        // we want to analyze only properties without initializers
        localProperties.retainAll { it.fir.initializer == null && it.fir.delegate == null }
        if (localProperties.isEmpty()) return
        val data = DataCollector(localProperties).getData(graph)
        val reporterVisitor = UninitializedPropertyReporter(data, localProperties, reporter)
        graph.traverse(TraverseDirection.Forward, reporterVisitor)
    }

    private class UninitializedPropertyReporter(
        val data: Map<CFGNode<*>, PropertyInitializationInfo>,
        val localProperties: Set<FirPropertySymbol>,
        val reporter: DiagnosticReporter
    ) : ControlFlowGraphVisitorVoid() {
        override fun visitNode(node: CFGNode<*>) {}

        override fun visitQualifiedAccessNode(node: QualifiedAccessNode) {
            val reference = node.fir.calleeReference as? FirResolvedNamedReference ?: return
            val symbol = reference.resolvedSymbol as? FirPropertySymbol ?: return
            if (symbol !in localProperties) return
            val kind = data.getValue(node)[symbol] ?: EventOccurrencesRange.ZERO
            if (!kind.isDefinitelyVisited()) {
                node.fir.source?.let {
                    reporter.report(FirErrors.UNINITIALIZED_VARIABLE.on(it, symbol))
                }
            }
        }
    }
}
