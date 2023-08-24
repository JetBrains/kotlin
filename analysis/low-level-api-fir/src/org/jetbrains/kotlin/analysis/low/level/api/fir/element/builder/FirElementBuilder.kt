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
import org.jetbrains.kotlin.analysis.low.level.api.fir.file.structure.FirElementsRecorder
import org.jetbrains.kotlin.analysis.low.level.api.fir.lazy.resolve.declarationCanBeLazilyResolved
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.findSourceNonLocalFirDeclaration
import org.jetbrains.kotlin.analysis.utils.printer.parentOfType
import org.jetbrains.kotlin.analysis.utils.printer.parentsOfType
import org.jetbrains.kotlin.fir.FirAnnotationContainer
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.correspondingProperty
import org.jetbrains.kotlin.fir.declarations.FirCallableDeclaration
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.FirReceiverParameter
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.FirTypeParameter
import org.jetbrains.kotlin.fir.declarations.FirValueParameter
import org.jetbrains.kotlin.fir.declarations.utils.correspondingValueParameterFromPrimaryConstructor
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.psi
import org.jetbrains.kotlin.fir.resolve.providers.firProvider
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhase
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.isObjectLiteral
import org.jetbrains.kotlin.psi.psiUtil.parentsWithSelf
import org.jetbrains.kotlin.utils.exceptions.errorWithAttachment
import org.jetbrains.kotlin.utils.exceptions.withPsiEntry

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
                    deparenthesized.selectorExpression ?: errorWithAttachment("Incomplete code") {
                        withPsiEntry("psi", deparenthesized)
                    }
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
    ): FirElement? {
        return if (element is KtFile && element !is KtCodeFragment) {
            getOrBuildFirForKtFile(element)
        } else {
            getFirForNonKtFileElement(element, firResolveSession)
        }
    }

    private fun getOrBuildFirForKtFile(ktFile: KtFile): FirFile {
        val firFile = moduleComponents.firFileBuilder.buildRawFirFileWithCaching(ktFile)
        firFile.lazyResolveToPhase(FirResolvePhase.BODY_RESOLVE)
        return firFile
    }

    private fun getFirForNonKtFileElement(
        element: KtElement,
        firResolveSession: LLFirResolveSession,
    ): FirElement? {
        require(element !is KtFile || element is KtCodeFragment)

        if (!doKtElementHasCorrespondingFirElement(element)) {
            return null
        }

        getFirForElementInsideAnnotations(element, firResolveSession)?.let { return it }
        getFirForElementInsideTypes(element, firResolveSession)?.let { return it }

        val psi = getPsiAsFirElementSource(element) ?: return null
        val firFile = element.containingKtFile
        val fileStructure = moduleComponents.fileStructureCache.getFileStructure(firFile)

        val structureElement = fileStructure.getStructureElementFor(element)
        val mappings = structureElement.mappings
        return mappings.getFir(psi)
    }

    private inline fun <T : KtElement> getFirForNonBodyElement(
        element: KtElement,
        firResolveSession: LLFirResolveSession,
        anchorElementProvider: (KtElement) -> T?,
        declarationProvider: (T) -> KtDeclaration?,
        resolveAndFindFirForAnchor: (FirDeclaration, T) -> FirElement?,
    ): FirElement? {
        val anchorElement = anchorElementProvider(element) ?: return null
        val declaration = declarationProvider(anchorElement) ?: return null
        val nonLocalDeclaration = declaration.getNonLocalContainingOrThisDeclaration()
        if (declaration != nonLocalDeclaration) return null

        val firDeclaration = nonLocalDeclaration.findSourceNonLocalFirDeclaration(
            firFileBuilder = moduleComponents.firFileBuilder,
            provider = firResolveSession.useSiteFirSession.firProvider,
        )

        val anchorFir = resolveAndFindFirForAnchor(firDeclaration, anchorElement) ?: return null
        // We use identity comparison here intentionally to check that it is exactly the object we want to find
        if (element === anchorElement) return anchorFir

        return findElementInside(firElement = anchorFir, element = element, stopAt = anchorElement)
    }

    private fun KtAnnotationEntry.owner(): KtDeclaration? {
        val parent = parent
        val modifierList = parent as? KtModifierList ?: (parent as? KtAnnotation)?.parent as? KtModifierList ?: return null
        return modifierList.owner as? KtDeclaration
    }

    private fun getFirForElementInsideAnnotations(
        element: KtElement,
        firResolveSession: LLFirResolveSession,
    ): FirElement? = getFirForNonBodyElement(
        element = element,
        firResolveSession = firResolveSession,
        anchorElementProvider = { it.parentOfType<KtAnnotationEntry>(withSelf = true) },
        declarationProvider = { it.owner() },
        resolveAndFindFirForAnchor = { declaration, anchor -> declaration.resolveAndFindAnnotation(anchor, goDeep = true) },
    )

    private fun getFirForElementInsideTypes(
        element: KtElement,
        firResolveSession: LLFirResolveSession,
    ): FirElement? = getFirForNonBodyElement(
        element = element,
        firResolveSession = firResolveSession,
        anchorElementProvider = { it.parentsOfType<KtTypeReference>(withSelf = true).lastOrNull() },
        declarationProvider = { it.parent as? KtDeclaration },
        resolveAndFindFirForAnchor = { declaration, anchor -> declaration.resolveAndFindTypeRefAnchor(anchor) },
    )?.let { firElement ->
        if (firElement is FirReceiverParameter) {
            firElement.typeRef
        } else {
            firElement
        }
    }

    private fun findElementInside(firElement: FirElement, element: KtElement, stopAt: PsiElement): FirElement? {
        val elementToSearch = getPsiAsFirElementSource(element) ?: return null
        val mapping = FirElementsRecorder.recordElementsFrom(firElement, FirElementsRecorder())

        var current: PsiElement? = elementToSearch
        while (current != null && current != stopAt && current !is KtFile) {
            if (current is KtElement) {
                mapping[current]?.let { return it }
            }

            current = current.parent
        }

        return firElement
    }

    private fun FirDeclaration.resolveAndFindTypeRefAnchor(typeReference: KtTypeReference): FirElement? {
        lazyResolveToPhase(FirResolvePhase.ANNOTATIONS_ARGUMENTS_MAPPING)

        if (this is FirCallableDeclaration) {
            returnTypeRef.takeIf { it.psi == typeReference }?.let { return it }
            receiverParameter?.takeIf { it.typeRef.psi == typeReference }?.let { return it }
        }

        if (this is FirTypeParameter) {
            for (typeRef in bounds) {
                if (typeRef.psi == typeReference) {
                    return typeRef
                }
            }
        }

        return null
    }

    private fun FirDeclaration.resolveAndFindAnnotation(annotationEntry: KtAnnotationEntry, goDeep: Boolean = false): FirAnnotation? {
        lazyResolveToPhase(FirResolvePhase.ANNOTATIONS_ARGUMENTS_MAPPING)
        findAnnotation(annotationEntry)?.let { return it }

        if (this is FirProperty) {
            backingField?.findAnnotation(annotationEntry)?.let { return it }
            getter?.findAnnotation(annotationEntry)?.let { return it }
            setter?.findAnnotation(annotationEntry)?.let { return it }
            setter?.valueParameters?.first()?.findAnnotation(annotationEntry)?.let { return it }
        }

        return when {
            !goDeep -> null
            this is FirProperty -> correspondingValueParameterFromPrimaryConstructor?.fir?.resolveAndFindAnnotation(annotationEntry)
            this is FirValueParameter -> correspondingProperty?.resolveAndFindAnnotation(annotationEntry)
            else -> null
        }
    }

    private fun FirAnnotationContainer.findAnnotation(
        annotationEntry: KtAnnotationEntry,
    ): FirAnnotation? = annotations.find { it.psi == annotationEntry }

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
    return getNonLocalContainingDeclaration(parentsWithSelf, predicate)
}

internal fun getNonLocalContainingDeclaration(
    elementsToCheck: Sequence<PsiElement>,
    predicate: (KtDeclaration) -> Boolean = { true }
): KtDeclaration? {
    var candidate: KtDeclaration? = null

    fun propose(declaration: KtDeclaration) {
        if (candidate == null) {
            candidate = declaration
        }
    }

    for (parent in elementsToCheck) {
        candidate?.let { notNullCandidate ->
            if (parent is KtEnumEntry ||
                parent is KtCallableDeclaration &&
                !notNullCandidate.isPartOf(parent) ||
                parent is KtAnonymousInitializer ||
                parent is KtObjectLiteralExpression ||
                parent is KtCallElement ||
                parent is KtCodeFragment
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
