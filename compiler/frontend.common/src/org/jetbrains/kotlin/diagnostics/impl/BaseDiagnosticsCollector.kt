/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.diagnostics.impl

import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.diagnostics.DiagnosticBaseContext
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.KtDiagnostic
import org.jetbrains.kotlin.diagnostics.KtSourcelessDiagnosticFactory

abstract class BaseDiagnosticsCollector : DiagnosticReporter() {
    abstract val diagnostics: List<KtDiagnostic>
    abstract val diagnosticsByFilePath: Map<String?, List<KtDiagnostic>>

    abstract val rawReporter: RawReporter

    fun interface RawReporter {
        companion object {
            val DO_NOTHING: RawReporter = RawReporter { _, _ -> }
        }

        fun report(message: String, severity: CompilerMessageSeverity)

        fun reportError(message: String) {
            report(message, CompilerMessageSeverity.ERROR)
        }

        fun reportIfNeeded(
            factory: KtSourcelessDiagnosticFactory,
            message: String,
            context: DiagnosticBaseContext,
        ) {
            val refinedSeverity = factory.getEffectiveSeverity(context.languageVersionSettings)?.toCompilerMessageSeverity() ?: return
            report(message, refinedSeverity)
        }
    }
}
