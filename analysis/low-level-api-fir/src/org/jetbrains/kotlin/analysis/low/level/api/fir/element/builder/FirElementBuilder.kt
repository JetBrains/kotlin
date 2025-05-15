/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.element.builder

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiErrorElement
import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.low.level.api.fir.LLFirModuleResolveComponents
import org.jetbrains.kotlin.analysis.low.level.api.fir.file.structure.FileStructure
import org.jetbrains.kotlin.analysis.low.level.api.fir.file.structure.FirElementsRecorder
import org.jetbrains.kotlin.analysis.low.level.api.fir.file.structure.KtToFirMapping
import org.jetbrains.kotlin.analysis.low.level.api.fir.lazy.resolve.elementCanBeLazilyResolved
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.findSourceNonLocalFirDeclaration
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.parentsWithSelfCodeFragmentAware
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.requireTypeIntersectionWith
import org.jetbrains.kotlin.analysis.utils.printer.parentOfType
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.correspondingValueParameterFromPrimaryConstructor
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.resolve.providers.firProvider
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhase
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhaseRecursively
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.parentsWithSelf
import org.jetbrains.kotlin.utils.ThreadSafe
import org.jetbrains.kotlin.utils.exceptions.errorWithAttachment
import org.jetbrains.kotlin.utils.exceptions.withPsiEntry

/**
 * This class is responsible for mapping from [KtElement] to [FirElement]
 * using [FileStructure][org.jetbrains.kotlin.analysis.low.level.api.fir.file.structure.FileStructure].
 *
 * @see getOrBuildFirFor
 * @see org.jetbrains.kotlin.analysis.low.level.api.fir.file.structure.FileStructure
 * @see getNonLocalContainingOrThisElement
 */
@ThreadSafe
internal class FirElementBuilder(private val moduleComponents: LLFirModuleResolveComponents) {
    companion object {
        private fun getPsiAsFirElementSource(element: KtElement): KtElement? {
            val unwrappedElement = if (element is KtExpression) KtPsiUtil.safeDeparenthesize(element) else element
            return when (unwrappedElement) {
                is KtPropertyDelegate -> unwrappedElement.expression ?: element
                is KtQualifiedExpression if unwrappedElement.selectorExpression is KtCallExpression -> {
                    /*
                     KtQualifiedExpression with KtCallExpression in selector transformed in FIR to FirFunctionCall expression
                     Which will have a receiver as qualifier
                     */
                    unwrappedElement.selectorExpression ?: errorWithAttachment("Incomplete code") {
                        withPsiEntry("psi", unwrappedElement)
                    }
                }
                is KtValueArgument -> {
                    // null will be return in case of invalid KtValueArgument
                    unwrappedElement.getArgumentExpression()
                }
                is KtStringTemplateEntryWithExpression -> unwrappedElement.expression
                is KtUserType if unwrappedElement.parent is KtNullableType -> unwrappedElement.parent as KtNullableType
                else -> unwrappedElement
            }
        }

        private fun doKtElementHasCorrespondingFirElement(ktElement: KtElement): Boolean = when (ktElement) {
            is KtImportList -> false
            is KtFileAnnotationList -> false
            is KtAnnotation -> false
            else -> true
        }
    }

    /**
     * Returns a [FirElement] in its final resolved state.
     *
     * Note: that it isn't always [BODY_RESOLVE][FirResolvePhase.BODY_RESOLVE]
     * as not all declarations have types/bodies/etc. to resolve.
     *
     * For instance, [KtPackageDirective] has nothing to resolve,
     * so it will be returned as is ([FirPackageDirective][org.jetbrains.kotlin.fir.FirPackageDirective]),
     * with the [RAW_FIR][FirResolvePhase.RAW_FIR] phase.
     *
     * @return associated [FirElement] in final resolved state if it exists.
     *
     * @see getFirForElementInsideAnnotations
     * @see getFirForElementInsideTypes
     * @see getFirForElementInsideFileHeader
     */
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

        val nonLocalContainer = element.getNonLocalContainingOrThisElement()
        tryGetFirWithoutBodyResolve(nonLocalContainer, element)?.let { return it }

        val psi = getPsiAsFirElementSource(element) ?: return null
        val ktFile = element.containingKtFile
        val fileStructure = moduleComponents.fileStructureCache.getFileStructure(ktFile)

        val structureElement = fileStructure.getStructureElementFor(element, nonLocalContainer)
        val mappings = structureElement.mappings
        return mappings.getFir(psi)
    }

    /**
     * Provides a fast path for well-known psi-to-fir mappings to avoid [FirResolvePhase.BODY_RESOLVE] there it is possible.
     *
     * This optimization makes sense only for [nonLocalContainer]s which might have such expensive resolution.
     * For instance, there is no need to avoid [FirResolvePhase.BODY_RESOLVE] for dangling modifiers as they don't
     * have bodies, so effectively it is the same as [FirResolvePhase.ANNOTATION_ARGUMENTS].
     *
     * Declaration containers ([KtFile], [KtScript], [KtClassOrObject]) are resolved till [FirResolvePhase.ANNOTATION_ARGUMENTS]
     * by default in [FileStructure.getStructureElementFor],
     * so there is no need to use optimized search via [getFirForElementInsideAnnotations]/[getFirForElementInsideTypes]
     * as this is redundant work and effectively duplicate the logic of [KtToFirMapping].
     *
     * [KtClassOrObject] is not excluded yet as in some cases it might trigger additional resolution.
     * For instance, currently it resolves generated members (fake constructor, data class members, enum members, etc.).
     *
     * [KtEnumEntry] is not a declaration container as it is treated as callable in K2.
     *
     * @see getFirForElementInsideFileHeader
     * @see getFirForElementInsideAnnotations
     * @see getFirForElementInsideTypes
     */
    private fun tryGetFirWithoutBodyResolve(nonLocalContainer: KtElement?, element: KtElement): FirElement? = when (nonLocalContainer) {
        is KtScript -> null
        is KtFile -> getFirForElementInsideFileHeader(element)

        is KtDeclaration -> getFirForElementInsideAnnotations(element, nonLocalContainer)
            ?: getFirForElementInsideTypes(element, nonLocalContainer)

        else -> null
    }

    private inline fun <T : KtElement, E : PsiElement> getFirForNonBodyElement(
        element: KtElement,
        nonLocalDeclaration: KtDeclaration?,
        anchorElementProvider: (KtElement) -> T?,
        elementOwnerProvider: (T) -> E?,
        resolveAndFindFirForAnchor: (FirElementWithResolveState, T) -> FirElement?,
    ): FirElement? {
        val anchorElement = anchorElementProvider(element) ?: return null
        val elementOwner = elementOwnerProvider(anchorElement) ?: return null

        val firElementContainer = if (elementOwner is KtFile) {
            moduleComponents.firFileBuilder.buildRawFirFileWithCaching(elementOwner)
        } else {
            if (elementOwner != nonLocalDeclaration) return null

            nonLocalDeclaration.findSourceNonLocalFirDeclaration(
                firFileBuilder = moduleComponents.firFileBuilder,
                provider = moduleComponents.session.firProvider,
            )
        }

        val anchorFir = resolveAndFindFirForAnchor(firElementContainer, anchorElement) ?: return null
        // We use identity comparison here intentionally to check that it is exactly the object we want to find
        if (element === anchorElement) return anchorFir

        return findElementInside(firElement = anchorFir, element = element)
    }

    private fun PsiElement.annotationOwner(): KtAnnotated? {
        val modifierList = when (val parent = parent) {
            is KtModifierList -> parent
            is KtAnnotation -> return parent.annotationOwner()
            is KtFileAnnotationList -> return parent.parent as? KtFile
            else -> null
        }

        return modifierList?.owner as? KtDeclaration
    }

    private fun getFirForElementInsideAnnotations(
        element: KtElement,
        nonLocalDeclaration: KtDeclaration,
    ): FirElement? = getFirForNonBodyElement(
        element = element,
        nonLocalDeclaration = nonLocalDeclaration,
        anchorElementProvider = { it.parentsOfType<KtAnnotationEntry>(nonLocalDeclaration).firstOrNull() },
        elementOwnerProvider = { it.annotationOwner() },
        resolveAndFindFirForAnchor = { declaration, anchor -> declaration.resolveAndFindAnnotation(anchor, goDeep = true) },
    )

    private fun getFirForElementInsideTypes(
        element: KtElement,
        nonLocalDeclaration: KtDeclaration,
    ): FirElement? = getFirForNonBodyElement(
        element = element,
        nonLocalDeclaration = nonLocalDeclaration,
        anchorElementProvider = { it.parentsOfType<KtTypeReference>(nonLocalDeclaration).lastOrNull() },
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

    private fun getFirForElementInsideFileHeader(element: KtElement): FirElement? = getFirForNonBodyElement<KtElement, KtAnnotated>(
        element = element,
        nonLocalDeclaration = null,
        anchorElementProvider = { it.fileHeaderAnchorElement() },
        elementOwnerProvider = { it.containingKtFile },
        resolveAndFindFirForAnchor = { declaration, anchor ->
            declaration.requireTypeIntersectionWith<FirFile>()

            when (anchor) {
                is KtPackageDirective -> declaration.packageDirective
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
        return parentsWithSelf.find { it is KtPackageDirective || it is KtImportDirective } as? KtElement
    }

    private fun findElementInside(firElement: FirElement, element: KtElement): FirElement? {
        val elementToSearch = getPsiAsFirElementSource(element) ?: return null
        val mapping = FirElementsRecorder.recordElementsFrom(firElement, FirElementsRecorder())
        return KtToFirMapping.getFir(elementToSearch, moduleComponents.session, mapping)
    }

    private fun FirElementWithResolveState.resolveAndFindTypeRefAnchor(typeReference: KtTypeReference): FirElement? {
        requireTypeIntersectionWith<FirAnnotationContainer>()

        lazyResolveToPhase(FirResolvePhase.ANNOTATION_ARGUMENTS)

        when (this) {
            is FirCallableDeclaration -> {
                returnTypeRef.takeIf { it.psi == typeReference }?.let { return it }
                receiverParameter?.takeIf { it.typeRef.psi == typeReference }?.let { return it }
            }

            is FirTypeParameter -> {
                findTypeRefAnchor(typeReference)?.let { return it }
            }

            is FirClass -> {
                for (typeRef in superTypeRefs) {
                    if (typeRef.psi == typeReference) {
                        return typeRef
                    }
                }
            }

            is FirTypeAlias -> {
                expandedTypeRef.takeIf { it.psi == typeReference }?.let { return it }
            }
        }

        if (this is FirTypeParameterRefsOwner) {
            for (typeParameterRef in typeParameters) {
                typeParameterRef.findTypeRefAnchor(typeReference)?.let { return it }
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

private fun KtElement.isPartOf(callableDeclaration: KtCallableDeclaration): Boolean = when (this) {
    is KtPropertyAccessor -> this.property == callableDeclaration
    is KtParameter -> {
        val ownerDeclaration = ownerDeclaration
        ownerDeclaration == callableDeclaration || ownerDeclaration?.isPartOf(callableDeclaration) == true
    }
    is KtTypeParameter -> containingDeclaration == callableDeclaration
    else -> false
}

internal val KtTypeParameter.containingDeclaration: KtDeclaration?
    get() = (parent as? KtTypeParameterList)?.parent as? KtDeclaration

/**
 * Returns **true** if [this] element is a unit of resolution and can be treated as non-local.
 * The property is supposed to be used only in the pair with
 * [getNonLocalContainingOrThisElement] or [getNonLocalContainingOrThisDeclaration].
 *
 * @see getNonLocalContainingOrThisElement
 */
internal val KtElement.isAutonomousElement: Boolean
    get() = when (this) {
        is KtPropertyAccessor, is KtParameter, is KtTypeParameter -> false
        else -> true
    }

// TODO change predicate (KT-76271)
@KaImplementationDetail
fun PsiElement.getNonLocalContainingOrThisDeclaration(predicate: (KtDeclaration) -> Boolean = { true }): KtDeclaration? {
    return getNonLocalContainingOrThisElement { it is KtDeclaration && predicate(it) } as? KtDeclaration
}

/**
 * Returns the first non-local element from [parentsWithSelf] or [parentsWithSelfCodeFragmentAware]
 * (depends on [codeFragmentAware] flag) that contains the given elements, based on the specified predicate.
 *
 * The resulting element can be considered reachable at [RAW_FIR][FirResolvePhase.RAW_FIR] phase.
 *
 * @see org.jetbrains.kotlin.analysis.low.level.api.fir.file.structure.FileStructure
 */
internal fun PsiElement.getNonLocalContainingOrThisElement(
    codeFragmentAware: Boolean = false,
    predicate: (KtElement) -> Boolean = { true },
): KtElement? {
    var candidate: KtElement? = null

    val elementsToCheck = if (codeFragmentAware) {
        parentsWithSelfCodeFragmentAware
    } else {
        parentsWithSelf
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
                // The candidate turned out to be local. Let's find another one.
                candidate = null
            }
        }

        // A new candidate only needs to be proposed when `candidate` is null.
        if (candidate == null &&
            parent is KtElement &&
            elementCanBeLazilyResolved(parent, codeFragmentAware) &&
            predicate(parent)
        ) {
            if (codeFragmentAware && this.containingFile is KtCodeFragment) {
                candidate = parent
            } else {
                return parent
            }
        }
    }

    return candidate
}

private inline fun <reified T : KtElement> PsiElement.parentsOfType(stopDeclaration: KtDeclaration?): Sequence<T> {
    return parentsWithSelf.takeWhile { it !== stopDeclaration }.filterIsInstance<T>()
}
