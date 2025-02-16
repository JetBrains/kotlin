/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.config

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.diagnostics.impl.BaseDiagnosticsCollector
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext

object FirContextParametersLanguageVersionSettingsChecker : FirLanguageVersionSettingsChecker() {
    private val WARNING_STARTING_FROM_2_2: String = """
        Experimental context receivers are superseded by context parameters.
        Replace the '-Xcontext-receivers' compiler argument with '-Xcontext-parameters' and migrate to the new syntax.

        See the context parameters proposal for more details: https://kotl.in/context-parameters
        This warning will become an error in future releases.""".trimIndent()

    private val WARNING_BEFORE_2_2: String = """
        Experimental context receivers are deprecated and will be superseded by context parameters.
        Kotlin compiler version ${LanguageVersion.KOTLIN_2_2} will be the last version that supports context receivers.
        Consider migrating to extension receivers or regular parameters now.
        Alternatively, migrate directly to context parameters when Kotlin ${LanguageVersion.KOTLIN_2_2} is released.

        See the context parameters proposal for more details: https://kotl.in/context-parameters
        This warning will become an error in future releases.""".trimIndent()

    fun getMessage(languageVersionSettings: LanguageVersionSettings): String {
        return if (languageVersionSettings.languageVersion >= LanguageVersion.KOTLIN_2_2) {
            WARNING_STARTING_FROM_2_2
        } else {
            WARNING_BEFORE_2_2
        }
    }

    override fun check(context: CheckerContext, reporter: BaseDiagnosticsCollector.RawReporter) {
        if (!context.languageVersionSettings.supportsFeature(LanguageFeature.ContextReceivers)) {
            return
        }

        if (context.languageVersionSettings.supportsFeature(LanguageFeature.ContextParameters)) {
            reporter.reportError(
                "Experimental language features for context receivers and context parameters cannot be enabled at the same time. " +
                        "Remove the '-Xcontext-receivers' compiler argument."
            )
        } else if (context.languageVersionSettings.languageVersion >= LanguageVersion.KOTLIN_2_2) {
            reporter.reportWarning(WARNING_STARTING_FROM_2_2)
        }
    }
}