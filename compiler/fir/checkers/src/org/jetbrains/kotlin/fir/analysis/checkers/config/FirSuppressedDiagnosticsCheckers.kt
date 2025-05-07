/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.config

import org.jetbrains.kotlin.config.AnalysisFlags
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.Severity
import org.jetbrains.kotlin.diagnostics.rendering.BaseDiagnosticRendererFactory
import org.jetbrains.kotlin.diagnostics.rendering.RootDiagnosticRendererFactory
import org.jetbrains.kotlin.diagnostics.reportGlobal
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.diagnosticRendererFactory
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.languageVersionSettings

object FirSuppressedDiagnosticsCheckers : FirLanguageVersionSettingsChecker() {
    override fun check(context: CheckerContext, reporter: DiagnosticReporter) {
        val warningLevelMap = context.session.languageVersionSettings.getFlag(AnalysisFlags.warningLevels)
        if (warningLevelMap.isEmpty()) return

        val allDiagnosticFactories = context.session.diagnosticRendererFactory.allDiagnosticFactories.associateBy { it.name }

        for (diagnosticName in warningLevelMap.keys) {
            val diagnosticFactory = allDiagnosticFactories[diagnosticName]
            if (diagnosticFactory == null) {
                reporter.reportGlobal(FirErrors.INVALID_DIAGNOSTIC_NAME_FOR_GLOBAL_SUPPRESSION, diagnosticName, context)
                continue
            }
            if (diagnosticFactory.severity == Severity.ERROR) {
                reporter.reportGlobal(FirErrors.GLOBAL_ERROR_SEVERITY_CHANGE, diagnosticName, context)
            }
        }
    }
}
