/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.common.fir

import org.jetbrains.kotlin.cli.common.messages.*
import org.jetbrains.kotlin.diagnostics.DiagnosticUtils
import org.jetbrains.kotlin.diagnostics.KtDiagnostic
import org.jetbrains.kotlin.diagnostics.KtPsiDiagnostic
import org.jetbrains.kotlin.diagnostics.Severity
import org.jetbrains.kotlin.diagnostics.impl.BaseDiagnosticsCollector
import org.jetbrains.kotlin.diagnostics.rendering.RootDiagnosticRendererFactory
import java.io.Closeable
import java.io.File
import java.io.InputStreamReader

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
                            // TODO: bring KtSourceFile and KtSourceFileLinesMapping here and rewrite reporting via it to avoid code duplication
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

private class KtSourceFilePos(val line: Int, val column: Int, val lineContent: String?) {

    // NOTE: This method is used for presenting positions to the user
    override fun toString(): String = if (line < 0) "(offset: $column line unknown)" else "($line,$column)"

    companion object {
        val NONE = KtSourceFilePos(-1, -1, null)
    }
}

private class SequentialFilePositionFinder(file: File) : Closeable {

    private var reader: InputStreamReader = file.reader(/* TODO: select proper charset */)

    private var currentLineContent: String? = null
    private val buffer = CharArray(255)
    private var bufLength = -1
    private var bufPos = 0
    private var endOfStream = false
    private var skipNextLf = false

    private var charsRead = 0
    private var currentLine = 0

    // assuming that if called multiple times, calls should be sorted by ascending offset
    fun findNextPosition(offset: Int, withLineContents: Boolean = true): KtSourceFilePos {
        assert(offset >= charsRead - (currentLineContent?.length ?: 0))

        fun posInCurrentLine(): KtSourceFilePos? {
            val col = offset - (charsRead - currentLineContent!!.length - 1)/* beginning of line offset */ + 1 /* col is 1-based */
            return if (col <= currentLineContent!!.length)
                KtSourceFilePos(currentLine, col, if (withLineContents) currentLineContent else null)
            else null
        }

        if (offset < charsRead) {
            return posInCurrentLine()!!
        }

        while (true) {
            if (currentLineContent == null) {
                currentLineContent = readNextLine()
            }

            posInCurrentLine()?.let { return@findNextPosition it }

            if (endOfStream) return KtSourceFilePos(-1, offset, if (withLineContents) currentLineContent else null)

            currentLineContent = null
        }
    }

    private fun readNextLine() = buildString {
        while (true) {
            if (bufPos >= bufLength) {
                bufLength = reader.read(buffer)
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
        reader.close()
    }
}
