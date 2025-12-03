/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.config

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.report
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.CliDiagnostics
import org.jetbrains.kotlin.fir.isEnabled

object FirContextParametersLanguageVersionSettingsChecker : FirLanguageVersionSettingsChecker() {
    val DIAGNOSTIC_MESSAGE: String = """
        Experimental context receivers are superseded by context parameters.
        Replace the '-Xcontext-receivers' compiler argument with '-Xcontext-parameters' and migrate to the new syntax.

        See the context parameters proposal for more details: https://kotl.in/context-parameters""".trimIndent()

    context(context: CheckerContext)
    override fun check(reporter: DiagnosticReporter) {
        if (!LanguageFeature.ContextReceivers.isEnabled()) {
            return
        }

        val message = when {
            LanguageFeature.ContextParameters.isEnabled() -> "Experimental language features for context receivers and context parameters cannot be enabled at the same time. " +
                    "Remove the '-Xcontext-receivers' compiler argument."
            else -> DIAGNOSTIC_MESSAGE
        }
        reporter.report(CliDiagnostics.CONTEXT_PARAMETERS_ARE_DEPRECATED, message)
    }
}
