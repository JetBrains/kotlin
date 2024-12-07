/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.collectors.components

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.DeclarationCheckersDiagnosticComponent
import org.jetbrains.kotlin.fir.analysis.checkers.expression.ExpressionCheckersDiagnosticComponent
import org.jetbrains.kotlin.fir.analysis.checkers.type.TypeCheckersDiagnosticComponent
import org.jetbrains.kotlin.fir.analysis.collectors.DiagnosticCollectorComponents
import org.jetbrains.kotlin.fir.analysis.collectors.CliDiagnosticsCollector
import org.jetbrains.kotlin.fir.resolve.ScopeSession

object DiagnosticComponentsFactory {
    private fun createAllDiagnosticComponents(
        session: FirSession,
        reporter: DiagnosticReporter,
        mppKind: MppCheckerKind,
    ): DiagnosticCollectorComponents {
        val regularComponents = buildList {
            add(DeclarationCheckersDiagnosticComponent(session, reporter, mppKind))
            add(ExpressionCheckersDiagnosticComponent(session, reporter, mppKind))
            add(TypeCheckersDiagnosticComponent(session, reporter, mppKind))
            add(ControlFlowAnalysisDiagnosticComponent(session, reporter, mppKind))
            if (mppKind == MppCheckerKind.Common) {
                add(ErrorNodeDiagnosticCollectorComponent(session, reporter))
                add(LanguageVersionSettingsDiagnosticComponent(session, reporter))
            }
        }.toTypedArray()
        return DiagnosticCollectorComponents(regularComponents, ReportCommitterDiagnosticComponent(session, reporter))
    }

    fun create(
        session: FirSession,
        scopeSession: ScopeSession,
        mppKind: MppCheckerKind
    ): CliDiagnosticsCollector {
        return CliDiagnosticsCollector(session, scopeSession) { reporter ->
            createAllDiagnosticComponents(session, reporter, mppKind)
        }
    }
}
