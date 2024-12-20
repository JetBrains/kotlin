/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.pipeline.web

import org.jetbrains.kotlin.cli.common.messages.AnalyzerWithCompilerReport
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.js.reportCollectedDiagnostics
import org.jetbrains.kotlin.diagnostics.impl.BaseDiagnosticsCollector
import org.jetbrains.kotlin.fir.pipeline.ModuleCompilerAnalyzedOutput
import org.jetbrains.kotlin.ir.backend.js.ModulesStructure
import org.jetbrains.kotlin.psi.KtFile

open class AnalyzedFirOutput(val output: List<ModuleCompilerAnalyzedOutput>) {
    protected open fun checkSyntaxErrors(messageCollector: MessageCollector): Boolean = false

    fun reportCompilationErrors(
        moduleStructure: ModulesStructure,
        diagnosticsReporter: BaseDiagnosticsCollector,
        messageCollector: MessageCollector,
    ): Boolean {
        if (checkSyntaxErrors(messageCollector) || diagnosticsReporter.hasErrors) {
            reportCollectedDiagnostics(moduleStructure.compilerConfiguration, diagnosticsReporter, messageCollector)
            return true
        }

        return false
    }
}

class AnalyzedFirWithPsiOutput(
    output: List<ModuleCompilerAnalyzedOutput>,
    private val compiledFiles: List<KtFile>
) : AnalyzedFirOutput(output) {
    override fun checkSyntaxErrors(messageCollector: MessageCollector): Boolean {
        return compiledFiles.fold(false) { errorsFound, file ->
            AnalyzerWithCompilerReport.Companion.reportSyntaxErrors(file, messageCollector).isHasErrors or errorsFound
        }
    }
}
