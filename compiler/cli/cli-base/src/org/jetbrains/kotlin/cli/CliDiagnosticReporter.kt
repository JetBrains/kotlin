/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli

import org.jetbrains.kotlin.cli.common.cliDiagnosticsReporter
import org.jetbrains.kotlin.cli.common.diagnosticsCollector
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSourceLocation
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.config.messageCollector
import org.jetbrains.kotlin.diagnostics.DiagnosticContext
import org.jetbrains.kotlin.diagnostics.KtDiagnostic
import org.jetbrains.kotlin.diagnostics.KtSourcelessDiagnosticFactory
import org.jetbrains.kotlin.diagnostics.impl.BaseDiagnosticsCollector
import org.jetbrains.kotlin.diagnostics.impl.DiagnosticsCollectorImpl
import org.jetbrains.kotlin.diagnostics.report

/**
 * This is a special reporter intended to report various sourceless diagnostics from the CLI.
 */
class CliDiagnosticReporter(val configuration: CompilerConfiguration) {
    private val context = object : DiagnosticContext {
        override val languageVersionSettings: LanguageVersionSettings
            get() = configuration.languageVersionSettings

        override val containingFilePath: String? get() = null

        override fun isDiagnosticSuppressed(diagnostic: KtDiagnostic): Boolean = false
    }

    fun report(
        factory: KtSourcelessDiagnosticFactory,
        message: String,
        location: CompilerMessageSourceLocation? = null,
    ) {
        context(context) {
            configuration.diagnosticsCollector.report(factory, message, location)
        }
    }

    fun info(message: String, location: CompilerMessageSourceLocation? = null) {
        configuration.messageCollector.report(CompilerMessageSeverity.INFO, message, location)
    }

    fun log(message: String, location: CompilerMessageSourceLocation? = null) {
        configuration.messageCollector.report(CompilerMessageSeverity.LOGGING, message, location)
    }

    companion object {
        val DO_NOTHING: CliDiagnosticReporter = run {
            @OptIn(CompilerConfiguration.Internals::class)
            val configuration = CompilerConfiguration().apply {
                messageCollector = MessageCollector.NONE
                diagnosticsCollector = DiagnosticsCollectorImpl()
            }
            CliDiagnosticReporter(configuration).also {
                configuration.cliDiagnosticsReporter = it
            }
        }
    }
}
