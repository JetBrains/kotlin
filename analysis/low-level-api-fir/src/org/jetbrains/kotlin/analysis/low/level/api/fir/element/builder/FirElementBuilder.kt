/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.element.builder

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiErrorElement
import org.jetbrains.kotlin.analysis.api.impl.barebone.annotations.ThreadSafe
import org.jetbrains.kotlin.analysis.low.level.api.fir.LLFirModuleResolveComponents
import org.jetbrains.kotlin.analysis.low.level.api.fir.file.structure.FirElementsRecorder
import org.jetbrains.kotlin.analysis.low.level.api.fir.lazy.resolve.declarationCanBeLazilyResolved
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.findSourceNonLocalFirDeclaration
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.requireTypeIntersectionWith
import org.jetbrains.kotlin.analysis.utils.printer.parentOfType
import org.jetbrains.kotlin.analysis.utils.printer.parentsOfType
import org.jetbrains.kotlin.fir.FirAnnotationContainer
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirElementWithResolveState
import org.jetbrains.kotlin.fir.correspondingProperty
import org.jetbrains.kotlin.fir.declarations.FirCallableDeclaration
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.FirReceiverParameter
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.FirTypeParameter
import org.jetbrains.kotlin.fir.declarations.FirTypeParameterRef
import org.jetbrains.kotlin.fir.declarations.FirValueParameter
import org.jetbrains.kotlin.fir.declarations.utils.correspondingValueParameterFromPrimaryConstructor
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.psi
import org.jetbrains.kotlin.fir.resolve.providers.firProvider
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhase
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhaseRecursively
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

    fun getOrBuildFirFor(element: KtElement): FirElement? {
        return if (element is KtFile && element !is KtCodeFragment) {
            getOrBuildFirForKtFile(element)
        } else {
            getFirForNonKtFileElement(element)
        }
    }

    private fun getOrBuildFirForKtFile(ktFile: KtFile): FirFile {
        val firFile = moduleComponents.firFileBuilder.buildRawFirFileWithCaching(ktFile)
        firFile.lazyResolveToPhaseRecursively(FirResolvePhase.BODY_RESOLVE)
        return firFile
    }

    private fun getFirForNonKtFileElement(element: KtElement): FirElement? {
        require(element !is KtFile || element is KtCodeFragment)

        if (!doKtElementHasCorrespondingFirElement(element)) {
            return null
        }

        getFirForElementInsideAnnotations(element)?.let { return it }
        getFirForElementInsideTypes(element)?.let { return it }
        getFirForElementInsideFileHeader(element)?.let { return it }

        val psi = getPsiAsFirElementSource(element) ?: return null
        val firFile = element.containingKtFile
        val fileStructure = moduleComponents.fileStructureCache.getFileStructure(firFile)

        val structureElement = fileStructure.getStructureElementFor(element)
        val mappings = structureElement.mappings
        return mappings.getFir(psi)
    }

    private inline fun <T : KtElement, E : PsiElement> getFirForNonBodyElement(
        element: KtElement,
        anchorElementProvider: (KtElement) -> T?,
        elementOwnerProvider: (T) -> E?,
        resolveAndFindFirForAnchor: (FirElementWithResolveState, T) -> FirElement?,
    ): FirElement? {
        val anchorElement = anchorElementProvider(element) ?: return null
        val elementOwner = elementOwnerProvider(anchorElement) ?: return null

        val firElementContainer = if (elementOwner is KtFile) {
            moduleComponents.firFileBuilder.buildRawFirFileWithCaching(elementOwner)
        } else {
            val nonLocalDeclaration = elementOwner.getNonLocalContainingOrThisDeclaration()
            if (elementOwner != nonLocalDeclaration) return null

            nonLocalDeclaration.findSourceNonLocalFirDeclaration(
                firFileBuilder = moduleComponents.firFileBuilder,
                provider = moduleComponents.session.firProvider,
            )
        }

        val anchorFir = resolveAndFindFirForAnchor(firElementContainer, anchorElement) ?: return null
        // We use identity comparison here intentionally to check that it is exactly the object we want to find
        if (element === anchorElement) return anchorFir

        return findElementInside(firElement = anchorFir, element = element, stopAt = anchorElement)
    }

    private fun KtAnnotationEntry.owner(): KtAnnotated? {
        val modifierList = when (val parent = parent) {
            is KtModifierList -> parent
            is KtAnnotation -> parent.parent as? KtModifierList
            is KtFileAnnotationList -> return parent.parent as? KtFile
            else -> null
        }

        return modifierList?.owner as? KtDeclaration
    }

    private fun getFirForElementInsideAnnotations(
        element: KtElement,
    ): FirElement? = getFirForNonBodyElement<KtAnnotationEntry, KtAnnotated>(
        element = element,
        anchorElementProvider = { it.parentOfType<KtAnnotationEntry>(withSelf = true) },
        elementOwnerProvider = { it.owner() },
        resolveAndFindFirForAnchor = { declaration, anchor -> declaration.resolveAndFindAnnotation(anchor, goDeep = true) },
    )

    private fun getFirForElementInsideTypes(element: KtElement): FirElement? = getFirForNonBodyElement<KtTypeReference, KtDeclaration>(
        element = element,
        anchorElementProvider = { it.parentsOfType<KtTypeReference>(withSelf = true).lastOrNull() },
        elementOwnerProvider = {
            when (val parent = it.parent) {
                is KtDeclaration -> parent
                is KtSuperTypeListEntry, is KtConstructorCalleeExpression, is KtTypeConstraint -> parent.parentOfType<KtDeclaration>()
                else -> null
            }
        },
        resolveAndFindFirForAnchor = { declaration, anchor -> declaration.resolveAndFindTypeRefAnchor(anchor) },
    )?.let { firElement ->
        if (firElement is FirReceiverParameter) {
            firElement.typeRef
        } else {
            firElement
        }
    }

    private fun getFirForElementInsideFileHeader(
        element: KtElement,
    ): FirElement? = getFirForNonBodyElement<KtElement, KtAnnotated>(
        element = element,
        anchorElementProvider = { it.fileHeaderAnchorElement() },
        elementOwnerProvider = { it.containingKtFile },
        resolveAndFindFirForAnchor = { declaration, anchor ->
            declaration.requireTypeIntersectionWith<FirFile>()

            when (anchor) {
                is KtPackageDirective -> declaration.packageDirective
                is KtFileAnnotationList -> declaration.annotationsContainer?.also { it.lazyResolveToPhase(FirResolvePhase.ANNOTATION_ARGUMENTS) }
                is KtImportDirective -> {
                    declaration.lazyResolveToPhase(FirResolvePhase.IMPORTS)
                    declaration.imports.find { it.psi == anchor }
                }
                else -> errorWithAttachment("Unexpected element type: ${anchor::class.simpleName}") {
                    withPsiEntry("anchor", anchor)
                }
            }
        },
    )

    private fun KtElement.fileHeaderAnchorElement(): KtElement? {
        /**
         * File annotations already covered by [getFirForElementInsideAnnotations], but we have to cover the list itself
         */
        if (this is KtFileAnnotationList) return this

        return parentsWithSelf.find { it is KtPackageDirective || it is KtImportDirective } as? KtElement
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

    private fun FirElementWithResolveState.resolveAndFindTypeRefAnchor(typeReference: KtTypeReference): FirElement? {
        requireTypeIntersectionWith<FirAnnotationContainer>()

        lazyResolveToPhase(FirResolvePhase.ANNOTATION_ARGUMENTS)

        if (this is FirCallableDeclaration) {
            returnTypeRef.takeIf { it.psi == typeReference }?.let { return it }
            receiverParameter?.takeIf { it.typeRef.psi == typeReference }?.let { return it }

            for (typeParameterRef in typeParameters) {
                typeParameterRef.findTypeRefAnchor(typeReference)?.let { return it }
            }
        }

        if (this is FirTypeParameter) {
            findTypeRefAnchor(typeReference)?.let { return it }
        }

        if (this is FirClass) {
            for (typeRef in superTypeRefs) {
                if (typeRef.psi == typeReference) {
                    return typeRef
                }
            }
        }

        return null
    }

    private fun FirTypeParameterRef.findTypeRefAnchor(typeReference: KtTypeReference): FirElement? {
        if (this !is FirTypeParameter) return null

        for (typeRef in bounds) {
            if (typeRef.psi == typeReference) {
                return typeRef
            }
        }

        return null
    }

    private fun FirElementWithResolveState.resolveAndFindAnnotation(
        annotationEntry: KtAnnotationEntry,
        goDeep: Boolean = false,
    ): FirAnnotation? {
        requireTypeIntersectionWith<FirAnnotationContainer>()

        lazyResolveToPhase(FirResolvePhase.ANNOTATION_ARGUMENTS)
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

internal val KtDeclaration.isAutonomousDeclaration: Boolean
    get() = when (this) {
        is KtPropertyAccessor, is KtParameter, is KtTypeParameter -> false
        else -> true
    }

internal fun PsiElement.getNonLocalContainingOrThisDeclaration(predicate: (KtDeclaration) -> Boolean = { true }): KtDeclaration? {
    return getNonLocalContainingDeclaration(parentsWithSelf, predicate)
}

internal fun getNonLocalContainingDeclaration(
    elementsToCheck: Sequence<PsiElement>,
    predicate: (KtDeclaration) -> Boolean = { true },
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
                parent is KtCodeFragment ||
                parent is PsiErrorElement
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
                is KtDestructuringDeclarationEntry -> propose(parent)
                is KtScriptInitializer -> propose(parent)
                is KtClassInitializer -> {
                    val container = parent.containingDeclaration
                    if (!container.isObjectLiteral() &&
                        declarationCanBeLazilyResolved(container) &&
                        predicate(parent)
                    ) {
                        propose(parent)
                    }
                }
                is KtDeclaration -> {
                    if (!parent.isAutonomousDeclaration) {
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
