/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.element.builder

import com.intellij.psi.PsiElement
import org.jetbrains.annotations.TestOnly
import org.jetbrains.kotlin.analysis.api.impl.barebone.annotations.ThreadSafe
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.FirModuleResolveState
import org.jetbrains.kotlin.analysis.low.level.api.fir.file.builder.FirFileBuilder
import org.jetbrains.kotlin.analysis.low.level.api.fir.file.builder.ModuleFileCache
import org.jetbrains.kotlin.analysis.low.level.api.fir.file.structure.FileStructureCache
import org.jetbrains.kotlin.analysis.low.level.api.fir.file.structure.FileStructureElement
import org.jetbrains.kotlin.analysis.low.level.api.fir.lazy.resolve.FirLazyDeclarationResolver
import org.jetbrains.kotlin.analysis.low.level.api.fir.lazy.resolve.declarationCanBeLazilyResolved
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.getElementTextInContext
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.isNonAnonymousClassOrObject
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.containingClassOrObject
import org.jetbrains.kotlin.psi.psiUtil.isAncestor
import org.jetbrains.kotlin.psi2ir.deparenthesize

/**
 * Maps [KtElement] to [FirElement]
 * Stateless, caches everything into [ModuleFileCache] & [FileStructureCache] passed into the function
 */
@ThreadSafe
internal class FirElementBuilder {
    fun getPsiAsFirElementSource(element: KtElement): KtElement? {
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
            deparenthesized is KtValueArgument -> {
                // null will be return in case of invalid KtValueArgument
                deparenthesized.getArgumentExpression()
            }
            deparenthesized is KtObjectLiteralExpression -> deparenthesized.objectDeclaration
            deparenthesized is KtStringTemplateEntryWithExpression -> deparenthesized.expression
            deparenthesized is KtUserType && deparenthesized.parent is KtNullableType -> deparenthesized.parent as KtNullableType
            else -> deparenthesized
        }
    }

    fun doKtElementHasCorrespondingFirElement(ktElement: KtElement): Boolean = when (ktElement) {
        is KtImportList -> false
        else -> true
    }

    fun getOrBuildFirFor(
        element: KtElement,
        firFileBuilder: FirFileBuilder,
        moduleFileCache: ModuleFileCache,
        fileStructureCache: FileStructureCache,
        firLazyDeclarationResolver: FirLazyDeclarationResolver,
        state: FirModuleResolveState,
    ): FirElement? = when (element) {
        is KtFile -> getOrBuildFirForKtFile(element, firFileBuilder, moduleFileCache, firLazyDeclarationResolver)
        else -> getOrBuildFirForNonKtFileElement(element, fileStructureCache, moduleFileCache, state)
    }

    private fun getOrBuildFirForKtFile(
        ktFile: KtFile,
        firFileBuilder: FirFileBuilder,
        moduleFileCache: ModuleFileCache,
        firLazyDeclarationResolver: FirLazyDeclarationResolver
    ): FirFile {
        val firFile = firFileBuilder.buildRawFirFileWithCaching(ktFile, moduleFileCache, preferLazyBodies = false)
        firLazyDeclarationResolver.lazyResolveFileDeclaration(
            firFile = firFile,
            moduleFileCache = moduleFileCache,
            toPhase = FirResolvePhase.BODY_RESOLVE,
            scopeSession = ScopeSession(),
            checkPCE = true
        )
        return firFile
    }

    private fun getOrBuildFirForNonKtFileElement(
        element: KtElement,
        fileStructureCache: FileStructureCache,
        moduleFileCache: ModuleFileCache,
        state: FirModuleResolveState,
    ): FirElement? {
        require(element !is KtFile)

        if (!doKtElementHasCorrespondingFirElement(element)) {
            return null
        }

        val firFile = element.containingKtFile
        val fileStructure = fileStructureCache.getFileStructure(firFile, moduleFileCache)

        val mappings = fileStructure.getStructureElementFor(element).mappings
        val psi = getPsiAsFirElementSource(element) ?: return null
        return mappings.getFirOfClosestParent(psi, state)
            ?: state.getOrBuildFirFile(firFile)
    }

    @TestOnly
    fun getStructureElementFor(
        element: KtElement,
        moduleFileCache: ModuleFileCache,
        fileStructureCache: FileStructureCache,
    ): FileStructureElement {
        val fileStructure = fileStructureCache.getFileStructure(element.containingKtFile, moduleFileCache)
        return fileStructure.getStructureElementFor(element)
    }
}

// TODO: simplify
internal inline fun PsiElement.getNonLocalContainingOrThisDeclaration(predicate: (KtDeclaration) -> Boolean = { true }): KtNamedDeclaration? {
    var container: PsiElement? = this
    while (container != null && container !is KtFile) {
        if (container is KtNamedDeclaration
            && (container.isNonAnonymousClassOrObject() || container is KtDeclarationWithBody || container is KtProperty || container is KtTypeAlias)
            && container !is KtPrimaryConstructor
            && declarationCanBeLazilyResolved(container)
            && container !is KtEnumEntry
            && container !is KtFunctionLiteral
            && container.containingClassOrObject !is KtEnumEntry
            && predicate(container)
        ) {
            return container
        }
        container = container.parent
    }
    return null
}

fun PsiElement.getNonLocalContainingInBodyDeclarationWith(): KtNamedDeclaration? =
    getNonLocalContainingOrThisDeclaration { declaration ->
        when (declaration) {
            is KtNamedFunction -> declaration.bodyExpression?.isAncestor(this) == true
            is KtProperty -> declaration.initializer?.isAncestor(this) == true ||
                    declaration.getter?.isAncestor(this) == true ||
                    declaration.setter?.isAncestor(this) == true
            else -> false
        }
    }
