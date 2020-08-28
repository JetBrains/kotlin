/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api.diagnostics

import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.idea.fir.low.level.api.file.builder.FirFileBuilder
import org.jetbrains.kotlin.idea.fir.low.level.api.file.builder.ModuleFileCache
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile
import java.util.concurrent.ConcurrentHashMap

internal class DiagnosticsCollector(
    private val firFileBuilder: FirFileBuilder,
    private val cache: ModuleFileCache,
) {
    private val diagnosticsForFile = ConcurrentHashMap<KtFile, DiagnosticsForFile>()

    fun getDiagnosticsFor(element: KtElement): List<Diagnostic> {
        val ktFile = element.containingKtFile
        val diagnostics = diagnosticsForFile.computeIfAbsent(ktFile) {
            val firFile = firFileBuilder.getFirFileResolvedToPhaseWithCaching(ktFile, cache, toPhase = FirResolvePhase.BODY_RESOLVE)
            DiagnosticsForFile.collectDiagnosticsForFile(firFile)
        }
        return diagnostics.getDiagnosticsFor(element)
    }
}

private class DiagnosticsForFile private constructor(private val diagnostics: Map<KtElement, List<Diagnostic>>) {
    fun getDiagnosticsFor(element: KtElement): List<Diagnostic> = diagnostics[element].orEmpty()

    companion object {
        /**
         * Collects diagnostics for given [firFile]
         * Should be called under [firFile]-based lock
         */
        fun collectDiagnosticsForFile(firFile: FirFile): DiagnosticsForFile {
            require(firFile.resolvePhase >= FirResolvePhase.BODY_RESOLVE) {
                "To collect diagnostics at least FirResolvePhase.BODY_RESOLVE is needed, but file ${firFile.name} was resolved to ${firFile.resolvePhase}"
            }
            return DiagnosticsForFile(FirIdeDiagnosticsCollector.collect(firFile))
        }
    }
}