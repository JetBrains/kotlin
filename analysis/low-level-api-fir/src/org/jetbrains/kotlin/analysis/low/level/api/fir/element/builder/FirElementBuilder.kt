/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.element.builder

import com.intellij.psi.PsiElement
import org.jetbrains.annotations.TestOnly
import org.jetbrains.kotlin.analysis.api.impl.barebone.annotations.ThreadSafe
import org.jetbrains.kotlin.analysis.low.level.api.fir.LLFirModuleResolveComponents
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.LLFirResolveSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.file.structure.FileStructureElement
import org.jetbrains.kotlin.analysis.low.level.api.fir.lazy.resolve.declarationCanBeLazilyResolved
import org.jetbrains.kotlin.analysis.utils.printer.getElementTextInContext
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhase
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.isAncestor
import org.jetbrains.kotlin.psi.psiUtil.isObjectLiteral
import org.jetbrains.kotlin.psi.psiUtil.parentsWithSelf

@ThreadSafe
internal class FirElementBuilder(
    private val moduleComponents: LLFirModuleResolveComponents,
) {
    companion object {
        fun getPsiAsFirElementSource(element: KtElement): KtElement? {
            val deparenthesized = if (element is KtExpression) KtPsiUtil.safeDeparenthesize(element) else element
            return when {
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
        firFile.lazyResolveToPhase(FirResolvePhase.BODY_RESOLVE)
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

        val structureElement = fileStructure.getStructureElementFor(element)
        val psi = getPsiAsFirElementSource(element) ?: return null
        val mappings = structureElement.mappings
        return mappings.getFirOfClosestParent(psi)
            ?: firResolveSession.getOrBuildFirFile(firFile)
    }

    @TestOnly
    fun getStructureElementFor(element: KtElement): FileStructureElement {
        val fileStructure = moduleComponents.fileStructureCache.getFileStructure(element.containingKtFile)
        return fileStructure.getStructureElementFor(element)
    }
}

private fun KtDeclaration.isPartOf(callableDeclaration: KtCallableDeclaration): Boolean = when (this) {
    is KtPropertyAccessor -> this.property == callableDeclaration
    is KtParameter -> {
        val ownerFunction = ownerFunction
        ownerFunction == callableDeclaration || ownerFunction?.isPartOf(callableDeclaration) == true
    }
    is KtTypeParameter -> containingDeclaration == callableDeclaration
    else -> false
}

internal val KtTypeParameter.containingDeclaration: KtDeclaration?
    get() = (parent as? KtTypeParameterList)?.parent as? KtDeclaration

internal val KtDeclaration.canBePartOfParentDeclaration: Boolean get() = this is KtPropertyAccessor || this is KtParameter || this is KtTypeParameter

internal fun PsiElement.getNonLocalContainingOrThisDeclaration(predicate: (KtDeclaration) -> Boolean = { true }): KtDeclaration? {
    var candidate: KtDeclaration? = null

    fun propose(declaration: KtDeclaration) {
        if (candidate == null) {
            candidate = declaration
        }
    }

    for (parent in parentsWithSelf) {
        candidate?.let { notNullCandidate ->
            if (parent is KtEnumEntry ||
                parent is KtCallableDeclaration &&
                !notNullCandidate.isPartOf(parent) ||
                parent is KtClassInitializer ||
                parent is KtObjectLiteralExpression ||
                parent is KtCallElement
            ) {
                // Candidate turned out to be local. Let's find another one.
                candidate = null
            }
        }

        // A new candidate only needs to be proposed when `candidate` is null.
        if (candidate == null) {
            when (parent) {
                is KtScript -> propose(parent)
                is KtDestructuringDeclaration -> propose(parent)
                is KtAnonymousInitializer -> {
                    val container = parent.containingDeclaration
                    if (container is KtClassOrObject &&
                        !container.isObjectLiteral() &&
                        declarationCanBeLazilyResolved(container) &&
                        predicate(parent)
                    ) {
                        propose(parent)
                    }
                }
                is KtDeclaration -> {
                    if (parent.canBePartOfParentDeclaration) {
                        if (predicate(parent)) {
                            propose(parent)
                        }
                    }

                    val isKindApplicable = when (parent) {
                        is KtClassOrObject -> !parent.isObjectLiteral()
                        is KtDeclarationWithBody, is KtProperty, is KtTypeAlias -> true
                        else -> false
                    }

                    if (isKindApplicable && declarationCanBeLazilyResolved(parent) && predicate(parent)) {
                        propose(parent)
                    }
                }
            }
        }
    }

    return candidate
}

@Suppress("unused") // Used in the IDE plugin
fun PsiElement.getNonLocalContainingInBodyDeclarationWith(): KtDeclaration? =
    getNonLocalContainingOrThisDeclaration { declaration ->
        when (declaration) {
            is KtNamedFunction -> declaration.bodyExpression?.isAncestor(this) == true
            is KtProperty -> declaration.initializer?.isAncestor(this) == true ||
                    declaration.getter?.bodyExpression?.isAncestor(this) == true ||
                    declaration.setter?.bodyExpression?.isAncestor(this) == true
            else -> false
        }
    }
