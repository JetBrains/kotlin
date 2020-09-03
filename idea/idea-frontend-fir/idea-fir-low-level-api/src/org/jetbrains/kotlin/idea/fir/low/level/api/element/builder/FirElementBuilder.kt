/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api.element.builder

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.resolve.firProvider
import org.jetbrains.kotlin.idea.fir.low.level.api.lazy.resolve.FirLazyDeclarationResolver
import org.jetbrains.kotlin.idea.fir.low.level.api.annotations.ThreadSafe
import org.jetbrains.kotlin.idea.fir.low.level.api.file.builder.FirFileBuilder
import org.jetbrains.kotlin.idea.fir.low.level.api.file.builder.ModuleFileCache
import org.jetbrains.kotlin.idea.fir.low.level.api.util.FirElementFinder
import org.jetbrains.kotlin.idea.fir.low.level.api.util.findNonLocalFirDeclaration
import org.jetbrains.kotlin.idea.util.getElementTextInContext
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.containingClassOrObject

/**
 * Maps [KtElement] to [FirElement]
 * Stateless, caches everything into [ModuleFileCache] & [PsiToFirCache] passed into the function
 */
@ThreadSafe
internal class FirElementBuilder(
    private val firFileBuilder: FirFileBuilder,
    private val firLazyDeclarationResolver: FirLazyDeclarationResolver,
) {
    fun getOrBuildFirFor(
        element: KtElement,
        moduleFileCache: ModuleFileCache,
        psiToFirCache: PsiToFirCache,
        toPhase: FirResolvePhase,
    ): FirElement {
        val ktFile = element.containingKtFile
        val firFile = firFileBuilder.buildRawFirFileWithCaching(ktFile, moduleFileCache)

        val containerFir = when (val container = element.getNonLocalContainingDeclarationWithFqName()) {
            is KtDeclaration -> container.findNonLocalFirDeclaration(firFileBuilder, firFile.session.firProvider, moduleFileCache)
            null -> firFile
            else -> error("Unsupported: ${container.text}")
        }
        firLazyDeclarationResolver.lazyResolveDeclaration(containerFir, moduleFileCache, toPhase, checkPCE = true)

        return psiToFirCache.getFir(element, containerFir, firFile)
    }
}


fun KtElement.getNonLocalContainingDeclarationWithFqName(): KtNamedDeclaration? {
    var container = parent
    while (container != null && container !is KtFile) {
        if (container is KtNamedDeclaration
            && (container is KtClassOrObject || container is KtDeclarationWithBody)
            && !KtPsiUtil.isLocal(container)
            && container.name != null
            && container !is KtEnumEntry
            && container.containingClassOrObject !is KtEnumEntry
        ) {
            return container
        }
        container = container.parent
    }
    return null
}