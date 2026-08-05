/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.diagnostics

import org.jetbrains.kotlin.analysis.low.level.api.fir.api.DiagnosticCheckerFilter
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.LLDiagnostic
import org.jetbrains.kotlin.analysis.low.level.api.fir.file.structure.FileStructureCache
import org.jetbrains.kotlin.psi.KtElement

internal class DiagnosticsCollector(private val fileStructureCache: FileStructureCache) {
    fun getDiagnosticsFor(element: KtElement, filter: DiagnosticCheckerFilter): List<LLDiagnostic> {
        val fileStructure = fileStructureCache.getFileStructure(element.containingKtFile)
        val structureElement = fileStructure.getStructureElementFor(element)
        val diagnostics = structureElement.diagnostics
        return diagnostics.diagnosticsFor(filter, element)
    }

    fun diagnosticsFor(element: KtElement, filter: DiagnosticCheckerFilter): Sequence<LLDiagnostic> {
        val fileStructure = fileStructureCache.getFileStructure(element.containingKtFile)
        return fileStructure.diagnostics(element, filter)
    }
}
