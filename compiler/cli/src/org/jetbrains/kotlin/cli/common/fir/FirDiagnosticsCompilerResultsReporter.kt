/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.common.fir

import org.jetbrains.kotlin.SequentialFilePositionFinder
import org.jetbrains.kotlin.cli.common.messages.*
import org.jetbrains.kotlin.diagnostics.DiagnosticUtils
import org.jetbrains.kotlin.diagnostics.KtDiagnostic
import org.jetbrains.kotlin.diagnostics.KtPsiDiagnostic
import org.jetbrains.kotlin.diagnostics.Severity
import org.jetbrains.kotlin.diagnostics.impl.BaseDiagnosticsCollector
import org.jetbrains.kotlin.diagnostics.rendering.RootDiagnosticRendererFactory
import java.io.File

object FirDiagnosticsCompilerResultsReporter {
    fun reportToMessageCollector(
        diagnosticsCollector: BaseDiagnosticsCollector,
        messageCollector: MessageCollector,
        renderDiagnosticName: Boolean
    ): Boolean {
        return reportByFile(diagnosticsCollector) { diagnostic, location ->
            reportDiagnosticToMessageCollector(diagnostic, location, messageCollector, renderDiagnosticName)
        }
    }

    fun throwFirstErrorAsException(
        diagnosticsCollector: BaseDiagnosticsCollector, messageRenderer: MessageRenderer = MessageRenderer.PLAIN_RELATIVE_PATHS
    ): Boolean {
        return reportByFile(diagnosticsCollector) { diagnostic, location ->
            throwErrorDiagnosticAsException(diagnostic, location, messageRenderer)
        }
    }

    fun reportByFile(
        diagnosticsCollector: BaseDiagnosticsCollector, report: (KtDiagnostic, CompilerMessageSourceLocation) -> Unit
    ): Boolean {
        var hasErrors = false
        for (filePath in diagnosticsCollector.diagnosticsByFilePath.keys) {
            val positionFinder: SequentialFilePositionFinder? by lazy(LazyThreadSafetyMode.NONE) {
                filePath?.let(::File)?.takeIf { it.isFile }?.let(::SequentialFilePositionFinder)
            }
            @Suppress("ConvertTryFinallyToUseCall")
            try {
                for (diagnostic in diagnosticsCollector.diagnosticsByFilePath[filePath].orEmpty().sortedWith(InFileDiagnosticsComparator)) {
                    when (diagnostic) {
                        is KtPsiDiagnostic -> {
                            val file = diagnostic.element.psi.containingFile
                            MessageUtil.psiFileToMessageLocation(
                                file,
                                file.name,
                                DiagnosticUtils.getLineAndColumnRange(file, diagnostic.textRanges)
                            )
                        }
                        else -> {
                            // NOTE: SequentialPositionFinder relies on the ascending order of the input offsets, so the code relies
                            // on the the appropriate sorting above
                            // Also the end offset is ignored, as it is irrelevant for the CLI reporting
                            positionFinder?.findNextPosition(DiagnosticUtils.firstRange(diagnostic.textRanges).startOffset)?.let { pos ->
                                MessageUtil.createMessageLocation(filePath, pos.lineContent, pos.line, pos.column, -1, -1)
                            }
                        }
                    }?.let { location ->
                        report(diagnostic, location)
                        hasErrors = hasErrors || diagnostic.severity == Severity.ERROR
                    }
                }
            } finally {
                positionFinder?.close()
            }
        }
        // TODO: for uncommenting, see comment in reportSpecialErrors
//        reportSpecialErrors(diagnostics)
        return hasErrors
    }

    @Suppress("UNUSED_PARAMETER", "unused")
    private fun reportSpecialErrors(diagnostics: Collection<KtDiagnostic>) {
        /*
         * TODO: handle next diagnostics when they will be supported in FIR:
         *  - INCOMPATIBLE_CLASS
         *  - PRE_RELEASE_CLASS
         *  - IR_WITH_UNSTABLE_ABI_COMPILED_CLASS
         *  - FIR_COMPILED_CLASS
         */
    }

    private fun reportDiagnosticToMessageCollector(
        diagnostic: KtDiagnostic,
        location: CompilerMessageSourceLocation,
        reporter: MessageCollector,
        renderDiagnosticName: Boolean
    ) {
        val severity = AnalyzerWithCompilerReport.convertSeverity(diagnostic.severity)
        val renderer = RootDiagnosticRendererFactory(diagnostic)

        val message = renderer.render(diagnostic)
        val textToRender = when (renderDiagnosticName) {
            true -> "[${diagnostic.factoryName}] $message"
            false -> message
        }

        reporter.report(severity, textToRender, location)
    }

    private fun throwErrorDiagnosticAsException(
        diagnostic: KtDiagnostic,
        location: CompilerMessageSourceLocation,
        messageRenderer: MessageRenderer
    ) {
        if (diagnostic.severity == Severity.ERROR) {
            val severity = AnalyzerWithCompilerReport.convertSeverity(diagnostic.severity)
            val renderer = RootDiagnosticRendererFactory(diagnostic)
            val diagnosticText = messageRenderer.render(severity, renderer.render(diagnostic), location)
            throw IllegalStateException("${diagnostic.factory.name}: $diagnosticText")
        }
    }

    private object InFileDiagnosticsComparator : Comparator<KtDiagnostic> {
        override fun compare(o1: KtDiagnostic, o2: KtDiagnostic): Int {
            val range1 = DiagnosticUtils.firstRange(o1.textRanges)
            val range2 = DiagnosticUtils.firstRange(o2.textRanges)

            return if (range1 != range2) {
                DiagnosticUtils.TEXT_RANGE_COMPARATOR.compare(range1, range2)
            } else o1.factory.name.compareTo(o2.factory.name)
        }
    }
}

fun BaseDiagnosticsCollector.reportToMessageCollector(messageCollector: MessageCollector, renderDiagnosticName: Boolean) {
    FirDiagnosticsCompilerResultsReporter.reportToMessageCollector(this, messageCollector, renderDiagnosticName)
}