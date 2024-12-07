/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.diagnostics

import org.jetbrains.kotlin.analysis.low.level.api.fir.api.DiagnosticCheckerFilter
import org.jetbrains.kotlin.analysis.low.level.api.fir.file.builder.ModuleFileCache
import org.jetbrains.kotlin.analysis.low.level.api.fir.file.structure.FileStructureCache
import org.jetbrains.kotlin.diagnostics.KtPsiDiagnostic
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile

internal class DiagnosticsCollector(private val fileStructureCache: FileStructureCache) {
    fun getDiagnosticsFor(element: KtElement, filter: DiagnosticCheckerFilter): List<KtPsiDiagnostic> {
        val fileStructure = fileStructureCache.getFileStructure(element.containingKtFile)
        val structureElement = fileStructure.getStructureElementFor(element)
        val diagnostics = structureElement.diagnostics
        return diagnostics.diagnosticsFor(filter, element)
    }

    fun collectDiagnosticsForFile(ktFile: KtFile, filter: DiagnosticCheckerFilter): List<KtPsiDiagnostic> {
        val fileStructure = fileStructureCache.getFileStructure(ktFile)
        return fileStructure.getAllDiagnosticsForFile(filter)
    }
}
