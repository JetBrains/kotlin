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
    /**
     * @see org.jetbrains.kotlin.analysis.low.level.api.fir.state.LLDiagnosticProvider.diagnostics
     */
    fun diagnostics(element: KtElement, filter: DiagnosticCheckerFilter, isRecursive: Boolean): Sequence<LLDiagnostic> {
        val fileStructure = fileStructureCache.getFileStructure(element.containingKtFile)
        if (isRecursive) {
            return fileStructure.diagnostics(element, filter)
        }

        val structureElement = fileStructure.getStructureElementFor(element)
        return structureElement.diagnostics.diagnosticsFor(filter, element).asSequence()
    }
}
