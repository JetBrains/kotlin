/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api

import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.FirFunction
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.idea.caches.project.IdeaModuleInfo
import org.jetbrains.kotlin.idea.fir.low.level.api.element.builder.FirTowerDataContextCollector
import org.jetbrains.kotlin.idea.fir.low.level.api.element.builder.PsiToFirCache
import org.jetbrains.kotlin.idea.fir.low.level.api.providers.FirIdeProvider
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile

internal class FirModuleResolveStateForCompletion(
    private val originalState: FirModuleResolveStateImpl
) : FirModuleResolveState() {
    override val moduleInfo: IdeaModuleInfo get() = originalState.moduleInfo
    override val firIdeSourcesSession: FirSession get() = originalState.firIdeSourcesSession
    override val firIdeLibrariesSession: FirSession get() = originalState.firIdeSourcesSession

    private val psiToFirCache = PsiToFirCache(originalState.fileCache)

    override fun getSessionFor(moduleInfo: IdeaModuleInfo): FirSession =
        originalState.getSessionFor(moduleInfo)

    override fun getOrBuildFirFor(element: KtElement, toPhase: FirResolvePhase): FirElement {
        getCachedMappingForCompletion(element)?.let { return it }
        return originalState.elementBuilder.getOrBuildFirFor(
            element,
            originalState.fileCache,
            psiToFirCache,
            toPhase
        )
    }

    override fun recordPsiToFirMappingsForCompletionFrom(fir: FirDeclaration, firFile: FirFile, ktFile: KtFile) {
        psiToFirCache.recordElementsForCompletionFrom(fir, firFile, ktFile)
    }

    override fun getCachedMappingForCompletion(element: KtElement): FirElement? {
        psiToFirCache.getCachedMapping(element)?.let { return it }
        originalState.psiToFirCache.getCachedMapping(element)?.let { return it }
        return null
    }

    override fun <D : FirDeclaration> resolvedFirToPhase(declaration: D, toPhase: FirResolvePhase): D {
        return originalState.resolvedFirToPhase(declaration, toPhase)
    }

    override fun lazyResolveFunctionForCompletion(
        firFunction: FirFunction<*>,
        containerFirFile: FirFile,
        firIdeProvider: FirIdeProvider,
        toPhase: FirResolvePhase,
        towerDataContextCollector: FirTowerDataContextCollector
    ) {
        originalState.lazyResolveFunctionForCompletion(firFunction, containerFirFile, firIdeProvider, toPhase, towerDataContextCollector)
    }

    override fun getDiagnostics(element: KtElement): List<Diagnostic> {
        error("Diagnostics should not be retrieved in completion")
    }
}