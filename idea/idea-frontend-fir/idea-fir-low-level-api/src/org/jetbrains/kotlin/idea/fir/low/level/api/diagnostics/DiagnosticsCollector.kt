/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api.diagnostics

import org.jetbrains.kotlin.fir.analysis.diagnostics.FirPsiDiagnostic
import org.jetbrains.kotlin.idea.fir.low.level.api.api.DiagnosticCheckerFilter
import org.jetbrains.kotlin.idea.fir.low.level.api.file.builder.ModuleFileCache
import org.jetbrains.kotlin.idea.fir.low.level.api.file.structure.FileStructureCache
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile

internal class DiagnosticsCollector(
    private val fileStructureCache: FileStructureCache,
    private val cache: ModuleFileCache,
) {
    fun getDiagnosticsFor(element: KtElement, filter: DiagnosticCheckerFilter): List<FirPsiDiagnostic<*>> {
        val fileStructure = fileStructureCache.getFileStructure(element.containingKtFile, cache)
        val structureElement = fileStructure.getStructureElementFor(element)
        val diagnostics = structureElement.diagnostics
        return diagnostics.diagnosticsFor(filter, element)
    }

    fun collectDiagnosticsForFile(ktFile: KtFile, filter: DiagnosticCheckerFilter): Collection<FirPsiDiagnostic<*>> {
        val fileStructure = fileStructureCache.getFileStructure(ktFile, cache)
        return fileStructure.getAllDiagnosticsForFile(filter)
    }
}