/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api.diagnostics

import com.intellij.psi.PsiElement
import com.intellij.util.SmartList
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirPsiDiagnostic
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.idea.fir.low.level.api.api.DiagnosticCheckerFilter
import org.jetbrains.kotlin.idea.fir.low.level.api.file.builder.LockProvider

internal class FileStructureElementDiagnostics(
    private val firFile: FirFile,
    private val lockProvider: LockProvider<FirFile>,
    private val retriever: FileStructureElementDiagnosticRetriever
) {
    private val diagnosticByCommonCheckers: FileStructureElementDiagnosticList by lazy {
        retriever.retrieve(firFile, FileStructureElementDiagnosticsCollector.USUAL_COLLECTOR, lockProvider)
    }

    private val diagnosticByExtendedCheckers: FileStructureElementDiagnosticList by lazy {
        retriever.retrieve(firFile, FileStructureElementDiagnosticsCollector.EXTENDED_COLLECTOR, lockProvider)
    }

    fun diagnosticsFor(filter: DiagnosticCheckerFilter, element: PsiElement): List<FirPsiDiagnostic<*>> =
        SmartList<FirPsiDiagnostic<*>>().apply {
            if (filter.runCommonCheckers) {
                addAll(diagnosticByCommonCheckers.diagnosticsFor(element))
            }
            if (filter.runExtendedCheckers) {
                addAll(diagnosticByExtendedCheckers.diagnosticsFor(element))
            }
        }


    inline fun forEach(filter: DiagnosticCheckerFilter, action: (List<FirPsiDiagnostic<*>>) -> Unit) {
        if (filter.runCommonCheckers) {
            diagnosticByCommonCheckers.forEach(action)
        }
        if (filter.runExtendedCheckers) {
            diagnosticByExtendedCheckers.forEach(action)
        }
    }
}