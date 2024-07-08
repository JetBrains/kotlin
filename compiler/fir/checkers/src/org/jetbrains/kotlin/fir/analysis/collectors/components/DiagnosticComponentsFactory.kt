/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.collectors.components

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.CheckerSessionKind
import org.jetbrains.kotlin.fir.analysis.checkers.CheckersCornerCase
import org.jetbrains.kotlin.fir.analysis.collectors.DiagnosticCollectorComponents
import org.jetbrains.kotlin.fir.analysis.collectors.SimpleDiagnosticsCollector
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.SessionHolder

object DiagnosticComponentsFactory {
    sealed class CompilationMode {
        data class Platform(val declarationSiteSessionHolder: SessionHolder?) : CompilationMode()
        data object Metadata : CompilationMode()
    }

    @OptIn(CheckersCornerCase::class)
    fun createAllDiagnosticComponents(
        session: FirSession,
        reporter: DiagnosticReporter,
        mode: CompilationMode,
    ): DiagnosticCollectorComponents = when (mode) {
        is CompilationMode.Platform -> DiagnosticCollectorComponents(
            commonComponents = createCheckers(CheckerSessionKind.DeclarationSite, session, reporter),
            expectComponents = createCheckers(CheckerSessionKind.DeclarationSiteForExpectsPlatformForOthers, session, reporter),
            regularComponents = createCheckers(CheckerSessionKind.Platform, session, reporter)
                .plus(ErrorNodeDiagnosticCollectorComponent(session, reporter))
                .plus(LanguageVersionSettingsDiagnosticComponent(session, reporter)),
            ReportCommitterDiagnosticComponent(session, reporter),
            mode.declarationSiteSessionHolder,
        )
        is CompilationMode.Metadata -> DiagnosticCollectorComponents(
            commonComponents = emptyList(),
            expectComponents = createCheckers(CheckerSessionKind.DeclarationSiteForExpectsPlatformForOthers, session, reporter),
            regularComponents = createCheckers(CheckerSessionKind.Platform, session, reporter),
            reportCommitter = ReportCommitterDiagnosticComponent(session, reporter),
            declarationSiteSessionHolder = null,
        )
    }

    private fun createCheckers(kind: CheckerSessionKind, session: FirSession, reporter: DiagnosticReporter) = buildList {
        add(DeclarationCheckersDiagnosticComponent(session, reporter, kind))
        add(ExpressionCheckersDiagnosticComponent(session, reporter, kind))
        add(TypeCheckersDiagnosticComponent(session, reporter, kind))
        add(ControlFlowAnalysisDiagnosticComponent(session, reporter, kind))
    }

    private operator fun DiagnosticCollectorComponents.plus(other: DiagnosticCollectorComponents) =
        DiagnosticCollectorComponents(
            commonComponents + other.commonComponents,
            expectComponents + other.expectComponents,
            regularComponents + other.regularComponents,
            reportCommitter, declarationSiteSessionHolder,
        )

    fun create(
        session: FirSession,
        scopeSession: ScopeSession,
        mode: CompilationMode,
    ): SimpleDiagnosticsCollector {
        return SimpleDiagnosticsCollector(session, scopeSession) { reporter ->
            createAllDiagnosticComponents(session, reporter, mode)
        }
    }


}
