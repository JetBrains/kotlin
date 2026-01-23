/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.collectors.components

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.cfa.FirCapturedMutableVariablesAnalyzer
import org.jetbrains.kotlin.fir.analysis.cfa.util.PropertyInitializationInfoData
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.DeclarationCheckers
import org.jetbrains.kotlin.fir.analysis.checkersComponent
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.resolve.dfa.controlFlowGraph

class ControlFlowAnalysisDiagnosticComponentCopy(
    session: FirSession,
    reporter: DiagnosticReporter,
    declarationCheckers: DeclarationCheckers,
) : AbstractDiagnosticCollectorComponent(session, reporter) {
    constructor(session: FirSession, reporter: DiagnosticReporter, mppKind: MppCheckerKind) : this(
        session,
        reporter,
        when (mppKind) {
            MppCheckerKind.Common -> session.checkersComponent.commonDeclarationCheckers
            MppCheckerKind.Platform -> session.checkersComponent.platformDeclarationCheckers
        }
    )
    private fun analyze(declaration: FirControlFlowGraphOwner, context: CheckerContext) {
        context(context, reporter) {
            val graph = declaration.controlFlowGraphReference?.controlFlowGraph ?: return

            val collector = ControlFlowAnalysisDiagnosticComponent.LocalPropertyCollector().apply { declaration.acceptChildren(this, graph.subGraphs.toSet()) }
            val properties = collector.properties

            val data = PropertyInitializationInfoData(properties, collector.conditionallyInitializedProperties, receiver = null, graph)
            FirCapturedMutableVariablesAnalyzer.analyze(data)
        }
    }

    override fun visitAnonymousFunction(anonymousFunction: FirAnonymousFunction, data: CheckerContext) {
        analyze(anonymousFunction, data)
    }
}
