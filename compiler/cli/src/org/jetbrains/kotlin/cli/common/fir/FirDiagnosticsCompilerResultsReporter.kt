/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.common.fir

import org.jetbrains.kotlin.AbstractKtSourceElement
import org.jetbrains.kotlin.KtLightSourceElement
import org.jetbrains.kotlin.KtPsiSourceElement
import org.jetbrains.kotlin.cli.common.messages.*
import org.jetbrains.kotlin.diagnostics.*
import org.jetbrains.kotlin.diagnostics.impl.BaseDiagnosticsCollector
import org.jetbrains.kotlin.diagnostics.rendering.DefaultErrorMessages
import org.jetbrains.kotlin.diagnostics.rendering.RootDiagnosticRendererFactory
import java.io.Closeable
import java.io.File
import java.io.InputStreamReader

object FirDiagnosticsCompilerResultsReporter {
    fun reportDiagnostics(diagnostics: Collection<KtDiagnostic>, reporter: MessageCollector, renderDiagnosticName: Boolean): Boolean {
        var hasErrors = false
        for (diagnostic in diagnostics.sortedWith(DiagnosticComparator)) {
            hasErrors = reportDiagnostic(diagnostic, reporter, renderDiagnosticName) || hasErrors
        }
        reportSpecialErrors(diagnostics)
        return hasErrors
    }

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
            for (diagnostic in diagnosticsCollector.diagnosticsByFilePath[filePath].orEmpty().sortedWith(SingleFileDiagnosticComparator)) {
                var filePositionFinder: SequentialFilePositionFinder? = null
                try {
                    when {
                        diagnostic is KtPsiDiagnostic -> diagnostic.element.location(diagnostic)
                        else -> {
                            if (filePositionFinder == null) {
                                filePositionFinder = SequentialFilePositionFinder(filePath)
                            }
                            // NOTE: SequentialPositionFinder relies on the ascending order of the input offsets, so the code relies
                            // on the the appropriate sorting above
                            // Also the end offset is ignored, as it is irrelevant for the CLI reporting
                            val position =
                                filePositionFinder.findNextPosition(DiagnosticUtils.firstRange(diagnostic.textRanges).startOffset)
                            MessageUtil.createMessageLocation(filePath, position.lineContent, position.line, position.column, -1, -1)
                        }
                    }?.let { location ->
                        report(diagnostic, location)
                        hasErrors = hasErrors || diagnostic.severity == Severity.ERROR
                    }
                } finally {
                    filePositionFinder?.close()
                }
            }
        }
//        reportSpecialErrors(diagnostics)
        return hasErrors
    }

    @Suppress("UNUSED_PARAMETER")
    private fun reportSpecialErrors(diagnostics: Collection<KtDiagnostic>) {
        /*
         * TODO: handle next diagnostics when they will be supported in FIR:
         *  - INCOMPATIBLE_CLASS
         *  - PRE_RELEASE_CLASS
         *  - IR_WITH_UNSTABLE_ABI_COMPILED_CLASS
         *  - FIR_COMPILED_CLASS
         */
    }

    private fun reportDiagnostic(diagnostic: KtDiagnostic, reporter: MessageCollector, renderDiagnosticName: Boolean): Boolean {
        if (!diagnostic.isValid) return false
        diagnostic.location()?.let {
            reportDiagnosticToMessageCollector(diagnostic, it, reporter, renderDiagnosticName)
        }
        return diagnostic.severity == Severity.ERROR
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

    private fun KtDiagnostic.location(): CompilerMessageSourceLocation? = when (val element = element) {
        is KtPsiSourceElement -> element.location(this)
        is KtLightSourceElement -> element.location(this)
        else -> element.genericLocation(this)
    }

    private fun KtPsiSourceElement.location(diagnostic: KtDiagnostic): CompilerMessageSourceLocation? {
        val file = psi.containingFile
        return MessageUtil.psiFileToMessageLocation(file, file.name, DiagnosticUtils.getLineAndColumnRange(file, diagnostic.textRanges))
    }

    @Suppress("UNUSED_PARAMETER")
    private fun KtLightSourceElement.location(diagnostic: KtDiagnostic): CompilerMessageSourceLocation? {
        // TODO: support light tree
        return null
    }

    @Suppress("UNUSED_PARAMETER")
    private fun AbstractKtSourceElement.genericLocation(diagnostic: KtDiagnostic): CompilerMessageSourceLocation? {
        // TODO: support generic location
        return null
    }

    private object DiagnosticComparator : Comparator<KtDiagnostic> {
        override fun compare(o1: KtDiagnostic, o2: KtDiagnostic): Int {
            val element1 = o1.element
            val element2 = o1.element
            // TODO: support light tree (likely obsolete, processing LT files in other code path)
            if (element1 !is KtPsiSourceElement || element2 !is KtPsiSourceElement) return 0

            val file1 = element1.psi.containingFile
            val file2 = element2.psi.containingFile
            val path1 = file1.viewProvider.virtualFile.path
            val path2 = file2.viewProvider.virtualFile.path
            if (path1 != path2) return path1.compareTo(path2)

            val range1 = DiagnosticUtils.firstRange(o1.textRanges)
            val range2 = DiagnosticUtils.firstRange(o2.textRanges)

            return if (range1 != range2) {
                DiagnosticUtils.TEXT_RANGE_COMPARATOR.compare(range1, range2)
            } else o1.factory.name.compareTo(o2.factory.name)
        }
    }

    private object SingleFileDiagnosticComparator : Comparator<KtDiagnostic> {
        override fun compare(o1: KtDiagnostic, o2: KtDiagnostic): Int {
            val range1 = DiagnosticUtils.firstRange(o1.textRanges)
            val range2 = DiagnosticUtils.firstRange(o2.textRanges)

            return if (range1 != range2) {
                DiagnosticUtils.TEXT_RANGE_COMPARATOR.compare(range1, range2)
            } else o1.factory.name.compareTo(o2.factory.name)
        }
    }

    class SequentialFilePositionFinder(private val filePath: String?) : Closeable {

        // TODO: verify if returning NONE on invalid files is the desired behavior (instead of throwing IOError)
        private var reader: InputStreamReader? = filePath?.let(::File)?.takeIf { it.isFile }?.reader(/* TODO: select proper charset */)

        private var currentLineContent: String? = null
        private val buffer = CharArray(255)
        private var bufLength = -1
        private var bufPos = 0
        private var endOfStream = false
        private var skipNextLf = false

        private var charsRead = 0
        private var currentLine = 0

        // assuming that if called multiple times, calls should be sorted by ascending offset
        @Synchronized
        fun findNextPosition(offset: Int, withLineContents: Boolean = true): PsiDiagnosticUtils.LineAndColumn {
            if (offset < 0 || reader == null) return PsiDiagnosticUtils.LineAndColumn.NONE

            assert(offset >= charsRead)

            while (true) {
                if (currentLineContent == null) {
                    currentLineContent = readNextLine()
                }

                val col = offset - (charsRead - currentLineContent!!.length - 1)/* beginning of line offset */ + 1 /* col is 1-based */
                if (col <= currentLineContent!!.length) {
                    return PsiDiagnosticUtils.LineAndColumn(currentLine, col, if (withLineContents) currentLineContent else null)
                }

                if (endOfStream) return PsiDiagnosticUtils.LineAndColumn.NONE

                currentLineContent = null
            }
        }

        private fun readNextLine() = buildString {
            while (true) {
                if (bufPos >= bufLength) {
                    bufLength = reader!!.read(buffer)
                    bufPos = 0
                    if (bufLength < 0) {
                        endOfStream = true
                        break
                    }
                } else {
                    val c = buffer[bufPos++]
                    charsRead++
                    when {
                        c == '\n' && skipNextLf -> {
                            skipNextLf = false
                        }
                        c == '\n' || c == '\r' -> {
                            currentLine++
                            skipNextLf = c == '\r'
                            break
                        }
                        else -> {
                            append(c)
                            skipNextLf = false
                        }
                    }
                }
            }
        }

        override fun close() {
            reader?.close()
        }
    }
}

fun BaseDiagnosticsCollector.reportToMessageCollector(messageCollector: MessageCollector, renderDiagnosticName: Boolean) {
    FirDiagnosticsCompilerResultsReporter.reportToMessageCollector(this, messageCollector, renderDiagnosticName)
}