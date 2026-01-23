/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.collectors.components

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.cfa.FirCapturedMutableVariablesAnalyzer
import org.jetbrains.kotlin.fir.analysis.cfa.util.PropertyInitializationInfoData
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.resolve.dfa.controlFlowGraph

class CapturedMutableVariablesDiagnosticComponent(
    session: FirSession,
    reporter: DiagnosticReporter,
) : AbstractDiagnosticCollectorComponent(session, reporter) {
    private fun analyze(declaration: FirControlFlowGraphOwner, context: CheckerContext) {
        context(context, reporter) {
            val graph = declaration.controlFlowGraphReference?.controlFlowGraph ?: return

            val collector = ControlFlowAnalysisDiagnosticComponent.LocalPropertyCollector().apply { declaration.acceptChildren(this, graph.subGraphs.toSet()) }

            val data =
                PropertyInitializationInfoData(collector.properties, collector.conditionallyInitializedProperties, receiver = null, graph)
            FirCapturedMutableVariablesAnalyzer.analyze(data)
        }
    }

    override fun visitAnonymousFunction(anonymousFunction: FirAnonymousFunction, data: CheckerContext) {
        analyze(anonymousFunction, data)
    }
}
