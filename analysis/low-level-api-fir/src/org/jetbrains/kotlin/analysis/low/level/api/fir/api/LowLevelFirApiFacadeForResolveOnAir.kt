/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.api

import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.analysis.low.level.api.fir.LLFirResolveSessionDepended
import org.jetbrains.kotlin.analysis.low.level.api.fir.element.builder.FileTowerProvider
import org.jetbrains.kotlin.analysis.low.level.api.fir.element.builder.FirTowerContextProvider
import org.jetbrains.kotlin.analysis.low.level.api.fir.element.builder.FirTowerDataContextAllElementsCollector
import org.jetbrains.kotlin.analysis.low.level.api.fir.element.builder.canBePartOfParentDeclaration
import org.jetbrains.kotlin.analysis.low.level.api.fir.element.builder.getNonLocalContainingOrThisDeclaration
import org.jetbrains.kotlin.analysis.low.level.api.fir.file.structure.FirElementsRecorder
import org.jetbrains.kotlin.analysis.low.level.api.fir.file.structure.KtToFirMapping
import org.jetbrains.kotlin.analysis.low.level.api.fir.lazy.resolve.RawFirNonLocalDeclarationBuilder
import org.jetbrains.kotlin.analysis.low.level.api.fir.lazy.resolve.RawFirReplacement
import org.jetbrains.kotlin.analysis.low.level.api.fir.lazy.resolve.buildFileFirAnnotation
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.LLFirResolvableModuleSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.llFirResolvableSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.llFirSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.state.LLFirResolvableResolveSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.FirElementFinder
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.errorWithFirSpecificEntries
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.originalDeclaration
import org.jetbrains.kotlin.analysis.utils.errors.buildErrorWithAttachment
import org.jetbrains.kotlin.analysis.utils.errors.withPsiEntry
import org.jetbrains.kotlin.analysis.utils.printer.parentOfType
import org.jetbrains.kotlin.analysis.utils.printer.parentsOfType
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.builder.buildFileAnnotationsContainer
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.realPsi
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.FirResolveContextCollector
import org.jetbrains.kotlin.fir.scopes.createImportingScopes
import org.jetbrains.kotlin.fir.scopes.kotlinScopeProvider
import org.jetbrains.kotlin.fir.visitors.FirVisitorVoid
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.anyDescendantOfType
import org.jetbrains.kotlin.psi.psiUtil.isAncestor
import org.jetbrains.kotlin.psi.psiUtil.parentsWithSelf

object LowLevelFirApiFacadeForResolveOnAir {
    private fun KtDeclaration.isApplicableForOnAirResolve(): Boolean = when (this) {
        is KtDestructuringDeclaration -> false
        else -> true
    }

    private fun PsiElement.onAirGetNonLocalContainingOrThisDeclaration(): KtDeclaration? {
        return getNonLocalContainingOrThisDeclaration { declaration ->
            declaration.isApplicableForOnAirResolve() && !declaration.canBePartOfParentDeclaration
        }
    }

    private fun recordOriginalDeclaration(targetDeclaration: KtDeclaration, originalDeclaration: KtDeclaration) {
        require(originalDeclaration.containingKtFile !== targetDeclaration.containingKtFile)
        val originalDeclarationParents = originalDeclaration.parentsOfType<KtDeclaration>().toList()
        val fakeDeclarationParents = targetDeclaration.parentsOfType<KtDeclaration>().toList()
        originalDeclarationParents.zip(fakeDeclarationParents) { original, fake ->
            fake.originalDeclaration = original
        }
    }

    fun <T : KtElement> onAirResolveElement(
        firResolveSession: LLFirResolveSession,
        place: T,
        elementToResolve: T,
    ): FirElement {
        require(firResolveSession is LLFirResolvableResolveSession)

        val declaration = runBodyResolveOnAir(
            firResolveSession = firResolveSession,
            originalPlace = place,
            replacementElement = elementToResolve,
            collector = null,
        )

        val expressionLocator = object : FirVisitorVoid() {
            var result: FirElement? = null
                private set

            override fun visitElement(element: FirElement) {
                if (element.realPsi == elementToResolve) result = element
                if (result != null) return
                element.acceptChildren(this)
            }
        }

        declaration.accept(expressionLocator)
        return expressionLocator.result ?: errorWithFirSpecificEntries("Resolved on-air element was not found in containing declaration") {
            withPsiEntry("place", place, firResolveSession::getModule)
            withPsiEntry("elementToResolve", elementToResolve, firResolveSession::getModule)
        }
    }

    fun getOnAirTowerDataContextProviderForTheWholeFile(
        firResolveSession: LLFirResolveSession,
        ktFile: KtFile,
    ): FirTowerContextProvider {
        val ktModule = firResolveSession.getModule(ktFile)
        val session = firResolveSession.getSessionFor(ktModule) as LLFirResolvableModuleSession
        val moduleComponents = session.moduleComponents

        val onAirFirFile = RawFirNonLocalDeclarationBuilder.buildNewFile(session, session.kotlinScopeProvider, ktFile)
        val collector = FirTowerDataContextAllElementsCollector()
        moduleComponents.firModuleLazyDeclarationResolver.runLazyDesignatedOnAirResolve(
            FirDesignationWithFile(emptyList(), onAirFirFile, onAirFirFile),
            collector,
        )

        return collector
    }

    fun getOnAirGetTowerContextProvider(
        firResolveSession: LLFirResolveSession,
        place: KtElement,
    ): FirTowerContextProvider {
        require(firResolveSession is LLFirResolvableResolveSession)

        return if (place is KtFile) {
            FileTowerProvider(place, onAirGetTowerContextForFile(firResolveSession, place))
        } else {
            val validPlace = PsiTreeUtil.findFirstParent(place, false) {
                it is KtElement && RawFirReplacement.isApplicableForReplacement(it)
            } as KtElement

            FirTowerDataContextAllElementsCollector().also {
                runBodyResolveOnAir(
                    firResolveSession = firResolveSession,
                    collector = it,
                    originalPlace = validPlace,
                    replacementElement = validPlace,
                )
            }
        }
    }

    private fun onAirGetTowerContextForFile(
        firResolveSession: LLFirResolvableResolveSession,
        file: KtFile,
    ): FirTowerDataContext {
        val module = firResolveSession.getModule(file)
        val session = firResolveSession.getSessionFor(module) as LLFirResolvableModuleSession
        val moduleComponents = session.moduleComponents

        val firFile = moduleComponents.firFileBuilder.buildRawFirFileWithCaching(file)

        val scopeSession = firResolveSession.getScopeSessionFor(session)
        return firFile.createTowerDataContext(scopeSession)
    }

    private fun FirFile.createTowerDataContext(scopeSession: ScopeSession): FirTowerDataContext {
        val importingScopes = createImportingScopes(this, moduleData.session, scopeSession)
        val fileScopeElements = importingScopes.map { it.asTowerDataElement(isLocal = false) }
        return FirTowerDataContext().addNonLocalTowerDataElements(fileScopeElements)
    }

    /**
     * Makes a copy of the original fir declaration and resolves it to the minimum required phase
     * depends on [elementToAnalyze] position inside a non-local containing declaration.
     *
     * If [elementToAnalyze] is non-local declaration then resolves it to [FirResolvePhase.BODY_RESOLVE].
     *
     * Resulted [LLFirResolveSession] have [FirTowerDataContext] for elements from the copy.
     */
    fun getFirResolveSessionForDependentCopy(
        originalFirResolveSession: LLFirResolveSession,
        originalKtFile: KtFile,
        elementToAnalyze: KtElement,
    ): LLFirResolveSession {
        require(originalFirResolveSession is LLFirResolvableResolveSession)
        require(elementToAnalyze !is KtFile) { "KtFile for dependency element not supported" }

        val minimalCopiedDeclaration = elementToAnalyze.getNonLocalContainingOrThisDeclaration {
            it.isApplicableForOnAirResolve()
        }

        val copiedNonLocalDeclaration = minimalCopiedDeclaration?.onAirGetNonLocalContainingOrThisDeclaration()
        if (copiedNonLocalDeclaration == null) {
            val towerDataContext = onAirGetTowerContextForFile(originalFirResolveSession, originalKtFile)
            val fileTowerProvider = FileTowerProvider(elementToAnalyze.containingKtFile, towerDataContext)
            return LLFirResolveSessionDepended(originalFirResolveSession, fileTowerProvider, ktToFirMapping = null)
        }

        val originalNonLocalDeclaration = PsiTreeUtil.findSameElementInCopy(copiedNonLocalDeclaration, originalKtFile)
            ?: buildErrorWithAttachment("Cannot find original function matching") {
                withPsiEntry("matchingPsi", elementToAnalyze, originalFirResolveSession::getModule)
                withPsiEntry("originalFile", originalKtFile, originalFirResolveSession::getModule)
            }

        recordOriginalDeclaration(targetDeclaration = copiedNonLocalDeclaration, originalDeclaration = originalNonLocalDeclaration)

        val collector = FirTowerDataContextAllElementsCollector()
        val copiedFirDeclaration = runBodyResolveOnAir(
            originalFirResolveSession,
            originalPlace = originalNonLocalDeclaration,
            replacementElement = copiedNonLocalDeclaration,
            collector = collector,
            forcedResolvePhase = requiredResolvePhase(minimalCopiedDeclaration, elementToAnalyze),
        )

        val mapping = KtToFirMapping(copiedFirDeclaration, FirElementsRecorder())
        return LLFirResolveSessionDepended(originalFirResolveSession, collector, mapping)
    }

    private fun tryResolveAsFileAnnotation(
        annotationEntry: KtAnnotationEntry,
        replacement: RawFirReplacement,
        firFile: FirFile,
        collector: FirResolveContextCollector? = null,
        firResolveSession: LLFirResolvableResolveSession,
    ): FirAnnotation {
        val annotationCall = buildFileFirAnnotation(
            session = firFile.moduleData.session,
            baseScopeProvider = firFile.moduleData.session.kotlinScopeProvider,
            fileAnnotation = annotationEntry,
            replacement = replacement
        )

        val fileAnnotationsContainer = buildFileAnnotationsContainer {
            moduleData = firFile.moduleData
            containingFileSymbol = firFile.symbol
            annotations += annotationCall
        }

        val llFirResolvableSession = firFile.llFirResolvableSession
            ?: buildErrorWithAttachment("FirFile session expected to be a resolvable session but was ${firFile.llFirSession::class.java}") {
                withEntry("firSession", firFile.llFirSession) { it.toString() }
            }

        val declarationResolver = llFirResolvableSession.moduleComponents.firModuleLazyDeclarationResolver
        declarationResolver.runLazyDesignatedOnAirResolve(
            FirDesignationWithFile(path = emptyList(), target = fileAnnotationsContainer, firFile),
            collector,
        )

        collector?.addFileContext(firFile, firFile.createTowerDataContext(firResolveSession.getScopeSessionFor(llFirResolvableSession)))

        // We should return annotation from fileAnnotationsContainer because the original annotation can be replaced
        return fileAnnotationsContainer.annotations.single()
    }

    /**
     * Creates a new fir declaration from closer non-local declaration based on [originalPlace] position.
     * The resulted [FirResolvePhase] depends on [forcedResolvePhase] or the position of [originalPlace] inside the non-local declaration.
     *
     * Note: the new declaration and its content won't be rebinded to the original.
     *
     * @see requiredResolvePhase
     */
    private fun runBodyResolveOnAir(
        firResolveSession: LLFirResolvableResolveSession,
        originalPlace: KtElement,
        replacementElement: KtElement,
        collector: FirResolveContextCollector?,
        forcedResolvePhase: FirResolvePhase? = null,
    ): FirElement {
        val minimalOriginalDeclarationToReplace = originalPlace.getNonLocalContainingOrThisDeclaration {
            it.isApplicableForOnAirResolve()
        }

        val originalFirFile = firResolveSession.getOrBuildFirFile(originalPlace.containingKtFile)

        val originalDeclaration = minimalOriginalDeclarationToReplace?.onAirGetNonLocalContainingOrThisDeclaration()
        if (originalDeclaration == null) {
            //It is possible that it is file annotation is going to resolve
            val annotationCall = originalPlace.parentOfType<KtAnnotationEntry>(withSelf = true)
            if (annotationCall != null) {
                return tryResolveAsFileAnnotation(
                    annotationEntry = annotationCall,
                    replacement = RawFirReplacement(from = originalPlace, to = replacementElement),
                    firFile = originalFirFile,
                    collector = collector,
                    firResolveSession = firResolveSession,
                )
            } else {
                errorWithFirSpecificEntries(
                    "Cannot find enclosing declaration for ${originalPlace::class.simpleName}",
                    psi = originalPlace,
                )
            }
        }

        val originalDesignationPath = FirElementFinder.collectDesignationPath(originalFirFile, originalDeclaration)
            ?: errorWithFirSpecificEntries(message = "Impossible to collect designation", fir = originalFirFile, psi = originalDeclaration)

        val originalFirDeclaration = originalDesignationPath.target
        val session = originalFirDeclaration.llFirResolvableSession ?: error("Expected resolvable session")

        /**
         * Special case for [getOnAirGetTowerContextProvider]. Can be dropped after KT-59498
         */
        val replacement = if (originalPlace === replacementElement) {
            RawFirReplacement(from = originalDeclaration, to = originalDeclaration)
        } else {
            RawFirReplacement(from = originalPlace, to = replacementElement)
        }

        val newFirDeclaration = RawFirNonLocalDeclarationBuilder.buildWithReplacement(
            session = session,
            scopeProvider = session.kotlinScopeProvider,
            designation = FirDesignation(originalDesignationPath.path, originalFirDeclaration),
            rootNonLocalDeclaration = originalDeclaration,
            replacement = replacement,
        )

        session.moduleComponents.firModuleLazyDeclarationResolver.runLazyDesignatedOnAirResolve(
            FirDesignationWithFile(originalDesignationPath.path, newFirDeclaration, originalFirFile),
            collector,
            forcedResolvePhase ?: requiredResolvePhase(minimalOriginalDeclarationToReplace, originalPlace),
        )

        return newFirDeclaration
    }

    private fun requiredResolvePhase(
        container: KtDeclaration,
        elementToReplace: PsiElement,
    ): FirResolvePhase = when {
        bodyResolveRequired(container, elementToReplace) -> FirResolvePhase.BODY_RESOLVE
        annotationMappingRequired(container, elementToReplace) -> FirResolvePhase.ANNOTATIONS_ARGUMENTS_MAPPING

        /**
         * Currently it is a minimal phase there we can collect [FirResolveContextCollector]
         */
        else -> FirResolvePhase.ARGUMENTS_OF_ANNOTATIONS
    }

    private fun annotationMappingRequired(
        container: KtDeclaration,
        elementToReplace: PsiElement,
    ): Boolean = elementToReplace.parentsWithSelf.takeWhile { it != container }.any {
        it is KtAnnotationEntry
    } || elementToReplace.anyDescendantOfType<KtAnnotationEntry>(
        canGoInside = {
            // We shouldn't go inside a class body, because class resolution won't process such elements
            // In case of container is class, we are interested only in elements outside its body
            it !is KtClassBody
        },
    )

    private fun bodyResolveRequired(container: KtDeclaration, elementToReplace: PsiElement): Boolean = when {
        container == elementToReplace -> true
        container is KtDeclarationWithBody -> container.bodyExpression?.isAncestor(elementToReplace)
        container is KtProperty -> container.delegateExpressionOrInitializer?.isAncestor(elementToReplace)
        container is KtParameter -> container.defaultValue?.isAncestor(elementToReplace)
        container is KtEnumEntry -> {
            container.initializerList?.isAncestor(elementToReplace) == true || container.body?.isAncestor(elementToReplace) == true
        }

        container is KtScript || container is KtAnonymousInitializer -> true
        else -> false
    } == true

    private fun isInBodyReplacement(ktDeclaration: KtDeclaration, replacement: RawFirReplacement): Boolean {
        fun check(container: KtElement?): Boolean {
            return container != null && container.isAncestor(replacement.from, true)
        }

        return when (ktDeclaration) {
            is KtNamedFunction -> check(ktDeclaration.bodyBlockExpression)
            is KtProperty -> {
                check(ktDeclaration.getter?.bodyBlockExpression)
                        || check(ktDeclaration.setter?.bodyBlockExpression)
                        || check(ktDeclaration.initializer)
            }
            is KtClassOrObject -> check(ktDeclaration.body)
            is KtScript -> check(ktDeclaration.blockExpression)
            is KtTypeAlias -> false
            else -> error("Not supported type ${ktDeclaration::class.simpleName}")
        }
    }
}
