/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.config

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportGlobal
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors

object FirContextReceiversLanguageVersionSettingsChecker : FirLanguageVersionSettingsChecker() {
    private val shouldSuggestContextParameters: Boolean = LanguageVersion.LATEST_STABLE >= LanguageVersion.KOTLIN_2_2

    val CONTEXT_RECEIVER_MESSAGE: String = if (shouldSuggestContextParameters) {
        """
            Experimental context receivers are superseded by context parameters.
            Replace the '-Xcontext-receivers' compiler argument with '-Xcontext-parameters' and migrate to the new syntax.

            See the context parameters proposal for more details: https://kotl.in/context-parameters
            This warning will become an error in future releases.""".trimIndent()
    } else {
        """
            Experimental context receivers are deprecated and will be superseded by context parameters.
            Kotlin compiler version ${LanguageVersion.KOTLIN_2_2} will be the last version that supports context receivers.
            Consider migrating to extension receivers or regular parameters now.
            Alternatively, migrate directly to context parameters when Kotlin ${LanguageVersion.KOTLIN_2_2} is released.

            See the context parameters proposal for more details: https://kotl.in/context-parameters
            This warning will become an error in future releases.""".trimIndent()
    }

    override fun check(context: CheckerContext, reporter: DiagnosticReporter) {
        if (!context.languageVersionSettings.supportsFeature(LanguageFeature.ContextReceivers)) {
            return
        }

        if (context.languageVersionSettings.supportsFeature(LanguageFeature.ContextParameters)) {
            reporter.reportGlobal(FirErrors.CONTEXT_RECEIVERS_AND_PARAMETERS_ENABLED_AT_THE_SAME_TIME, context)
        } else if (shouldSuggestContextParameters) {
            reporter.reportGlobal(FirErrors.CONTEXT_RECEIVER_ENABLED, CONTEXT_RECEIVER_MESSAGE, context)
        }
    }
}