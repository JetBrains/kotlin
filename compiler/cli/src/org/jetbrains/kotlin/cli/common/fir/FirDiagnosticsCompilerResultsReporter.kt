/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.common.fir

import org.jetbrains.kotlin.cli.common.messages.AnalyzerWithCompilerReport
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSourceLocation
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.common.messages.MessageUtil
import org.jetbrains.kotlin.diagnostics.DiagnosticUtils
import org.jetbrains.kotlin.diagnostics.Severity
import org.jetbrains.kotlin.fir.FirLightSourceElement
import org.jetbrains.kotlin.fir.FirPsiSourceElement
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirDefaultErrorMessages
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirDiagnostic

object FirDiagnosticsCompilerResultsReporter {
    fun reportDiagnostics(diagnostics: Collection<FirDiagnostic>, reporter: MessageCollector): Boolean {
        var hasErrors = false
        for (diagnostic in diagnostics.sortedWith(DiagnosticComparator)) {
            hasErrors = reportDiagnostic(diagnostic, reporter) || hasErrors
        }
        reportSpecialErrors(diagnostics)
        return hasErrors
    }

    @Suppress("UNUSED_PARAMETER")
    private fun reportSpecialErrors(diagnostics: Collection<FirDiagnostic>) {
        /*
         * TODO: handle next diagnostics when they will be supported in FIR:
         *  - INCOMPATIBLE_CLASS
         *  - PRE_RELEASE_CLASS
         *  - IR_WITH_UNSTABLE_ABI_COMPILED_CLASS
         *  - FIR_COMPILED_CLASS
         */
    }

    private fun reportDiagnostic(diagnostic: FirDiagnostic, reporter: MessageCollector): Boolean {
        if (!diagnostic.isValid) return false
        diagnostic.location()?.let { location ->
            val severity = AnalyzerWithCompilerReport.convertSeverity(diagnostic.severity)
            // TODO: support multiple maps with messages
            val renderer = FirDefaultErrorMessages.getRendererForDiagnostic(diagnostic)
            reporter.report(severity, renderer.render(diagnostic), location)
        }
        return diagnostic.severity == Severity.ERROR
    }

    private fun FirDiagnostic.location(): CompilerMessageSourceLocation? = when (val element = element) {
        is FirPsiSourceElement -> element.location(this)
        is FirLightSourceElement -> element.location(this)
    }

    private fun FirPsiSourceElement.location(diagnostic: FirDiagnostic): CompilerMessageSourceLocation? {
        val file = psi.containingFile
        return MessageUtil.psiFileToMessageLocation(file, file.name, DiagnosticUtils.getLineAndColumnRange(file, diagnostic.textRanges))
    }

    @Suppress("UNUSED_PARAMETER")
    private fun FirLightSourceElement.location(diagnostic: FirDiagnostic): CompilerMessageSourceLocation? {
        // TODO: support light tree
        return null
    }

    private object DiagnosticComparator : Comparator<FirDiagnostic> {
        override fun compare(o1: FirDiagnostic, o2: FirDiagnostic): Int {
            val element1 = o1.element
            val element2 = o1.element
            // TODO: support light tree
            if (element1 !is FirPsiSourceElement || element2 !is FirPsiSourceElement) return 0

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
}
