/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.diagnostics

import com.intellij.psi.PsiElement
import com.intellij.util.SmartList
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.DiagnosticCheckerFilter
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.LLDiagnostic

/**
 * Diagnostics of a single [FileStructureElement][org.jetbrains.kotlin.analysis.low.level.api.fir.file.structure.FileStructureElement],
 * computed lazily and separately per checker kind.
 *
 * Suppressed diagnostics are collected together with the reported ones and are distinguished by [LLDiagnostic.isSuppressed], so there is
 * no need for a separate collection pass to get them.
 */
internal class FileStructureElementDiagnostics(private val retriever: FileStructureElementDiagnosticRetriever) {
    private val diagnosticByDefaultCheckers: FileStructureElementDiagnosticList by lazy {
        retriever.retrieve(DiagnosticCheckerFilter.ONLY_DEFAULT_CHECKERS)
    }

    private val diagnosticByExtraCheckers: FileStructureElementDiagnosticList by lazy {
        retriever.retrieve(DiagnosticCheckerFilter.ONLY_EXTRA_CHECKERS)
    }

    private val diagnosticByExperimentalCheckers: FileStructureElementDiagnosticList by lazy {
        retriever.retrieve(DiagnosticCheckerFilter.ONLY_EXPERIMENTAL_CHECKERS)
    }

    fun diagnosticsFor(filter: DiagnosticCheckerFilter, element: PsiElement): List<LLDiagnostic> =
        SmartList<LLDiagnostic>().apply {
            if (filter.runDefaultCheckers) {
                addAll(diagnosticByDefaultCheckers.diagnosticsFor(element))
            }
            if (filter.runExtraCheckers) {
                addAll(diagnosticByExtraCheckers.diagnosticsFor(element))
            }
            if (filter.runExperimentalCheckers) {
                addAll(diagnosticByExperimentalCheckers.diagnosticsFor(element))
            }
        }

    inline fun forEach(filter: DiagnosticCheckerFilter, action: (List<LLDiagnostic>) -> Unit) {
        if (filter.runDefaultCheckers) {
            diagnosticByDefaultCheckers.forEach(action)
        }
        if (filter.runExtraCheckers) {
            diagnosticByExtraCheckers.forEach(action)
        }
        if (filter.runExperimentalCheckers) {
            diagnosticByExperimentalCheckers.forEach(action)
        }
    }
}
