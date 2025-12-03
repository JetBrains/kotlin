/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.config

import org.jetbrains.kotlin.config.AnalysisFlags
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.Severity
import org.jetbrains.kotlin.diagnostics.report
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.CliDiagnostics.ERROR_SEVERITY_CHANGED
import org.jetbrains.kotlin.fir.analysis.diagnostics.CliDiagnostics.MISSING_DIAGNOSTIC_NAME
import org.jetbrains.kotlin.fir.analysis.diagnostics.diagnosticRendererFactory
import org.jetbrains.kotlin.fir.languageVersionSettings

object FirSuppressedDiagnosticsCheckers : FirLanguageVersionSettingsChecker() {
    context(context: CheckerContext)
    override fun check(reporter: DiagnosticReporter) {
        val warningLevelMap = context.session.languageVersionSettings.getFlag(AnalysisFlags.warningLevels)
        if (warningLevelMap.isEmpty()) return

        val allDiagnosticFactories = context.session.diagnosticRendererFactory.allDiagnosticFactories.associateBy { it.name }

        for (diagnosticName in warningLevelMap.keys) {
            val diagnosticFactory = allDiagnosticFactories[diagnosticName]
            if (diagnosticFactory == null) {
                reporter.report(
                    MISSING_DIAGNOSTIC_NAME,
                    """Warning with name "$diagnosticName" does not exist"""
                )
                continue
            }
            if (diagnosticFactory.severity == Severity.ERROR) {
                reporter.report(
                    ERROR_SEVERITY_CHANGED,
                    """Diagnostic "$diagnosticName" is an error. Changing the severity of errors is prohibited"""
                )
            }
        }
    }
}
