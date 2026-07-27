/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.diagnostics

import com.intellij.psi.PsiElement
import com.intellij.util.SmartList
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.DiagnosticCheckerFilter
import org.jetbrains.kotlin.diagnostics.KtPsiDiagnostic

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

    private val diagnosticByDefaultCheckersIgnoringSuppression: FileStructureElementDiagnosticList by lazy {
        retriever.retrieve(DiagnosticCheckerFilter.ONLY_DEFAULT_CHECKERS, ignoreSuppression = true)
    }

    private val diagnosticByExtraCheckersIgnoringSuppression: FileStructureElementDiagnosticList by lazy {
        retriever.retrieve(DiagnosticCheckerFilter.ONLY_EXTRA_CHECKERS, ignoreSuppression = true)
    }

    private val diagnosticByExperimentalCheckersIgnoringSuppression: FileStructureElementDiagnosticList by lazy {
        retriever.retrieve(DiagnosticCheckerFilter.ONLY_EXPERIMENTAL_CHECKERS, ignoreSuppression = true)
    }

    fun diagnosticsFor(filter: DiagnosticCheckerFilter, element: PsiElement): List<KtPsiDiagnostic> =
        SmartList<KtPsiDiagnostic>().apply {
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

    // TODO(KT-86610): avoid recalculation between suppressed and not suppressed diagnostics
    inline fun forEach(filter: DiagnosticCheckerFilter, ignoreSuppression: Boolean, action: (List<KtPsiDiagnostic>) -> Unit) {
        if (filter.runDefaultCheckers) {
            if (ignoreSuppression) {
                diagnosticByDefaultCheckersIgnoringSuppression.forEach(action)
            } else {
                diagnosticByDefaultCheckers.forEach(action)
            }
        }
        if (filter.runExtraCheckers) {
            if (ignoreSuppression) {
                diagnosticByExtraCheckersIgnoringSuppression.forEach(action)
            } else {
                diagnosticByExtraCheckers.forEach(action)
            }
        }
        if (filter.runExperimentalCheckers) {
            if (ignoreSuppression) {
                diagnosticByExperimentalCheckersIgnoringSuppression.forEach(action)
            } else {
                diagnosticByExperimentalCheckers.forEach(action)
            }
        }
    }
}
