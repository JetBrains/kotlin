/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api.element.builder

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.idea.fir.low.level.api.lazy.resolve.FirLazyDeclarationResolver
import org.jetbrains.kotlin.idea.fir.low.level.api.annotations.ThreadSafe
import org.jetbrains.kotlin.idea.fir.low.level.api.file.builder.FirFileBuilder
import org.jetbrains.kotlin.idea.fir.low.level.api.file.builder.ModuleFileCache
import org.jetbrains.kotlin.idea.fir.low.level.api.file.structure.FileStructureCache
import org.jetbrains.kotlin.idea.util.getElementTextInContext
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.containingClassOrObject
import org.jetbrains.kotlin.psi2ir.deparenthesize

/**
 * Maps [KtElement] to [FirElement]
 * Stateless, caches everything into [ModuleFileCache] & [FileStructureCache] passed into the function
 */
@ThreadSafe
internal class FirElementBuilder {
    fun getPsiAsFirElementSource(element: KtElement): KtElement {
        val deparenthesized = if (element is KtPropertyDelegate) element.deparenthesize() else element
        return when {
            deparenthesized is KtParenthesizedExpression -> deparenthesized.deparenthesize()
            deparenthesized is KtPropertyDelegate -> deparenthesized.expression ?: element
            deparenthesized is KtQualifiedExpression && deparenthesized.selectorExpression is KtCallExpression -> {
                /*
                 KtQualifiedExpression with KtCallExpression in selector transformed in FIR to FirFunctionCall expression
                 Which will have a receiver as qualifier
                 */
                deparenthesized.selectorExpression ?: error("Incomplete code:\n${element.getElementTextInContext()}")
            }
            else -> deparenthesized
        }
    }

    fun getOrBuildFirFor(
        element: KtElement,
        moduleFileCache: ModuleFileCache,
        fileStructureCache: FileStructureCache,
    ): FirElement {
        val fileStructure = fileStructureCache.getFileStructure(element.containingKtFile, moduleFileCache)
        val mappings = fileStructure.getStructureElementFor(element).mappings

        val psi = getPsiAsFirElementSource(element)
        mappings[psi]?.let { return it }
        return psi.getFirOfClosestParent(mappings)?.second
            ?: error("FirElement is not found for:\n${element.getElementTextInContext()}")
    }
}

private fun KtElement.getFirOfClosestParent(cache: Map<KtElement, FirElement>): Pair<KtElement, FirElement>? {
    var current: PsiElement? = this
    while (current is KtElement) {
        val mappedFir = cache[current]
        if (mappedFir != null) {
            return current to mappedFir
        }
        current = current.parent
    }

    return null
}

fun KtElement.getNonLocalContainingOrThisDeclaration(): KtNamedDeclaration? {
    var container: PsiElement? = this
    while (container != null && container !is KtFile) {
        if (container is KtNamedDeclaration
            && (container is KtClassOrObject || container is KtDeclarationWithBody || container is KtProperty || container is KtTypeAlias)
            && container !is KtPrimaryConstructor
            && !KtPsiUtil.isLocal(container)
            && container !is KtEnumEntry
            && container.containingClassOrObject !is KtEnumEntry
        ) {
            return container
        }
        container = container.parent
    }
    return null
}