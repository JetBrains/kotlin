/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.config

import org.jetbrains.kotlin.config.AnalysisFlags
import org.jetbrains.kotlin.diagnostics.Severity
import org.jetbrains.kotlin.diagnostics.impl.BaseDiagnosticsCollector
import org.jetbrains.kotlin.diagnostics.rendering.BaseDiagnosticRendererFactory
import org.jetbrains.kotlin.diagnostics.rendering.RootDiagnosticRendererFactory
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.languageVersionSettings

object FirSuppressedDiagnosticsCheckers : FirLanguageVersionSettingsChecker() {
    override fun check(context: CheckerContext, reporter: BaseDiagnosticsCollector.RawReporter) {
        val globallySuppressedDiagnostics = context.session.languageVersionSettings.getFlag(AnalysisFlags.globallySuppressedDiagnostics)
        if (globallySuppressedDiagnostics.isEmpty()) return

        val allDiagnosticFactories = RootDiagnosticRendererFactory.factories
            .filterIsInstance<BaseDiagnosticRendererFactory>()
            .flatMap { it.MAP.factories }
            .associateBy { it.name }

        for (diagnosticName in globallySuppressedDiagnostics) {
            val diagnosticFactory = allDiagnosticFactories[diagnosticName]
            if (diagnosticFactory == null) {
                reporter.reportError("Warning with name \"$diagnosticName\" does not exists")
                continue
            }
            if (diagnosticFactory.severity == Severity.ERROR) {
                reporter.reportError("Diagnostic \"$diagnosticName\" is an error. Global suppression of errors is prohibited")
            }
        }
    }
}
