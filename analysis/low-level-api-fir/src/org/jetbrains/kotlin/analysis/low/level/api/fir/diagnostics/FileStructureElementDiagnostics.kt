/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.diagnostics

import com.intellij.psi.PsiElement
import com.intellij.util.SmartList
import org.jetbrains.kotlin.analysis.low.level.api.fir.LLFirModuleResolveComponents
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.DiagnosticCheckerFilter
import org.jetbrains.kotlin.diagnostics.KtPsiDiagnostic
import org.jetbrains.kotlin.fir.declarations.FirFile

internal class FileStructureElementDiagnostics(
    private val firFile: FirFile,
    private val retriever: FileStructureElementDiagnosticRetriever,
    private val moduleComponents: LLFirModuleResolveComponents,
) {
    private val diagnosticByCommonCheckers: FileStructureElementDiagnosticList by lazy {
        retriever.retrieve(firFile, FileStructureElementDiagnosticsCollector.USUAL_COLLECTOR, moduleComponents)
    }

    private val diagnosticByExtendedCheckers: FileStructureElementDiagnosticList by lazy {
        retriever.retrieve(firFile, FileStructureElementDiagnosticsCollector.EXTENDED_COLLECTOR, moduleComponents)
    }

    fun diagnosticsFor(filter: DiagnosticCheckerFilter, element: PsiElement): List<KtPsiDiagnostic> =
        SmartList<KtPsiDiagnostic>().apply {
            if (filter.runCommonCheckers) {
                addAll(diagnosticByCommonCheckers.diagnosticsFor(element))
            }
            if (filter.runExtendedCheckers) {
                addAll(diagnosticByExtendedCheckers.diagnosticsFor(element))
            }
        }


    inline fun forEach(filter: DiagnosticCheckerFilter, action: (List<KtPsiDiagnostic>) -> Unit) {
        if (filter.runCommonCheckers) {
            diagnosticByCommonCheckers.forEach(action)
        }
        if (filter.runExtendedCheckers) {
            diagnosticByExtendedCheckers.forEach(action)
        }
    }
}
