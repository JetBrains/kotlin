/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli

import org.jetbrains.kotlin.cli.common.diagnosticsCollector
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSourceLocation
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.config.messageCollector
import org.jetbrains.kotlin.diagnostics.DiagnosticContext
import org.jetbrains.kotlin.diagnostics.KtDiagnostic
import org.jetbrains.kotlin.diagnostics.KtSourcelessDiagnosticFactory
import org.jetbrains.kotlin.diagnostics.report

fun CompilerConfiguration.report(
    factory: KtSourcelessDiagnosticFactory,
    message: String,
    location: CompilerMessageSourceLocation? = null,
) {
    val context = object : DiagnosticContext {
        override val languageVersionSettings: LanguageVersionSettings
            get() = this@report.languageVersionSettings

        override val containingFilePath: String? get() = null

        override fun isDiagnosticSuppressed(diagnostic: KtDiagnostic): Boolean = false
    }

    context(context) {
        diagnosticsCollector.report(factory, message, location)
    }
}

fun CompilerConfiguration.reportInfo(message: String, location: CompilerMessageSourceLocation? = null) {
    messageCollector.report(CompilerMessageSeverity.INFO, message, location)
}

fun CompilerConfiguration.reportLog(message: String, location: CompilerMessageSourceLocation? = null) {
    messageCollector.report(CompilerMessageSeverity.LOGGING, message, location)
}

fun CompilerConfiguration.reportOutput(message: String, location: CompilerMessageSourceLocation? = null) {
    messageCollector.report(CompilerMessageSeverity.OUTPUT, message, location)
}
