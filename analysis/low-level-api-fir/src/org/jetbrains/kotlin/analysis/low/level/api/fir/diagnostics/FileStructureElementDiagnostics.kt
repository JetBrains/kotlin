/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.diagnostics

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.DiagnosticCheckerFilter
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.LLDiagnostic

/**
 * Diagnostics of a single [FileStructureElement][org.jetbrains.kotlin.analysis.low.level.api.fir.file.structure.FileStructureElement],
 * computed lazily and separately per checker kind.
 *
 * Suppressed diagnostics are collected together with the reported ones and are distinguished by [LLDiagnostic.isSuppressed], so there is
 * no need for a separate collection pass to get them.
 *
 * The returned sequences are lazy: a checker kind allowed by the [DiagnosticCheckerFilter] is only run once the consumer reaches its
 * diagnostics.
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

    /**
     * All diagnostics of the structure element, in no particular order.
     */
    fun diagnostics(filter: DiagnosticCheckerFilter): Sequence<LLDiagnostic> = sequence {
        if (filter.runDefaultCheckers) {
            yieldAll(diagnosticByDefaultCheckers.diagnostics())
        }

        if (filter.runExtraCheckers) {
            yieldAll(diagnosticByExtraCheckers.diagnostics())
        }

        if (filter.runExperimentalCheckers) {
            yieldAll(diagnosticByExperimentalCheckers.diagnostics())
        }
    }

    /**
     * Diagnostics reported on [element] itself, but not on its children.
     */
    fun directDiagnostics(filter: DiagnosticCheckerFilter, element: PsiElement): Sequence<LLDiagnostic> = sequence {
        if (filter.runDefaultCheckers) {
            yieldAll(diagnosticByDefaultCheckers.directDiagnostics(element))
        }

        if (filter.runExtraCheckers) {
            yieldAll(diagnosticByExtraCheckers.directDiagnostics(element))
        }

        if (filter.runExperimentalCheckers) {
            yieldAll(diagnosticByExperimentalCheckers.directDiagnostics(element))
        }
    }
}
