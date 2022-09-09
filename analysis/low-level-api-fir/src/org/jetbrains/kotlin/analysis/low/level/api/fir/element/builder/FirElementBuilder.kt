/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.element.builder

import com.intellij.psi.PsiElement
import org.jetbrains.annotations.TestOnly
import org.jetbrains.kotlin.analysis.api.impl.barebone.annotations.ThreadSafe
import org.jetbrains.kotlin.analysis.low.level.api.fir.LLFirModuleResolveComponents
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.LLFirResolveSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.file.structure.FileStructureElement
import org.jetbrains.kotlin.analysis.low.level.api.fir.lazy.resolve.declarationCanBeLazilyResolved
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.isNonAnonymousClassOrObject
import org.jetbrains.kotlin.analysis.utils.printer.getElementTextInContext
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.containingClassOrObject
import org.jetbrains.kotlin.psi.psiUtil.isAncestor
import org.jetbrains.kotlin.psi2ir.deparenthesize


@ThreadSafe
internal class FirElementBuilder(
    private val moduleComponents: LLFirModuleResolveComponents,
) {
    companion object {
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
    }


    fun doKtElementHasCorrespondingFirElement(ktElement: KtElement): Boolean = when (ktElement) {
        is KtImportList -> false
        else -> true
    }

    fun getOrBuildFirFor(
        element: KtElement,
        firResolveSession: LLFirResolveSession,
    ): FirElement? = when (element) {
        is KtFile -> getOrBuildFirForKtFile(element)
        else -> getOrBuildFirForNonKtFileElement(element, firResolveSession)
    }

    private fun getOrBuildFirForKtFile(ktFile: KtFile): FirFile {
        val firFile = moduleComponents.firFileBuilder.buildRawFirFileWithCaching(ktFile)
        moduleComponents.firModuleLazyDeclarationResolver.lazyResolveFileDeclaration(
            firFile = firFile,
            toPhase = FirResolvePhase.BODY_RESOLVE,
            scopeSession = moduleComponents.scopeSessionProvider.getScopeSession(),
            checkPCE = true
        )
        return firFile
    }

    private fun getOrBuildFirForNonKtFileElement(
        element: KtElement,
        firResolveSession: LLFirResolveSession,
    ): FirElement? {
        require(element !is KtFile)

        if (!doKtElementHasCorrespondingFirElement(element)) {
            return null
        }

        val firFile = element.containingKtFile
        val fileStructure = moduleComponents.fileStructureCache.getFileStructure(firFile)

        val mappings = fileStructure.getStructureElementFor(element).mappings
        val psi = getPsiAsFirElementSource(element) ?: return null
        return mappings.getFirOfClosestParent(psi, firResolveSession)
            ?: firResolveSession.getOrBuildFirFile(firFile)
    }

    @TestOnly
    fun getStructureElementFor(element: KtElement): FileStructureElement {
        val fileStructure = moduleComponents.fileStructureCache.getFileStructure(element.containingKtFile)
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
                    declaration.getter?.bodyExpression?.isAncestor(this) == true ||
                    declaration.setter?.bodyExpression?.isAncestor(this) == true
            else -> false
        }
    }
