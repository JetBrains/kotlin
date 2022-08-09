/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.api

import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.analysis.low.level.api.fir.DeclarationCopyBuilder.withBodyFrom
import org.jetbrains.kotlin.analysis.low.level.api.fir.LLFirResolveSessionDepended
import org.jetbrains.kotlin.analysis.low.level.api.fir.state.LLFirResolvableResolveSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.element.builder.FileTowerProvider
import org.jetbrains.kotlin.analysis.low.level.api.fir.element.builder.FirTowerContextProvider
import org.jetbrains.kotlin.analysis.low.level.api.fir.element.builder.FirTowerDataContextAllElementsCollector
import org.jetbrains.kotlin.analysis.low.level.api.fir.file.builder.runCustomResolveUnderLock
import org.jetbrains.kotlin.analysis.low.level.api.fir.file.structure.FirElementsRecorder
import org.jetbrains.kotlin.analysis.low.level.api.fir.file.structure.KtToFirMapping
import org.jetbrains.kotlin.analysis.low.level.api.fir.lazy.resolve.*
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.LLFirResolvableModuleSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.llFirResolvableSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.llFirSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.errorWithFirSpecificEntries
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.originalDeclaration
import org.jetbrains.kotlin.analysis.project.structure.getKtModule
import org.jetbrains.kotlin.analysis.utils.printer.getElementTextInContext
import org.jetbrains.kotlin.analysis.utils.printer.parentOfType
import org.jetbrains.kotlin.analysis.utils.printer.parentsOfType
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.realPsi
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.transformers.FirTypeResolveTransformer
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.FirTowerDataContextCollector
import org.jetbrains.kotlin.fir.scopes.createImportingScopes
import org.jetbrains.kotlin.fir.scopes.kotlinScopeProvider
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.fir.visitors.FirVisitorVoid
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.containingClassOrObject
import org.jetbrains.kotlin.psi.psiUtil.isAncestor
import org.jetbrains.kotlin.analysis.utils.errors.buildErrorWithAttachment
import org.jetbrains.kotlin.analysis.utils.errors.*

object LowLevelFirApiFacadeForResolveOnAir {

    private fun KtDeclaration.canBeEnclosingDeclaration(): Boolean = when (this) {
        is KtNamedFunction -> isTopLevel || containingClassOrObject?.isLocal == false
        is KtProperty -> isTopLevel || containingClassOrObject?.isLocal == false
        is KtClassOrObject -> !isLocal
        is KtTypeAlias -> isTopLevel() || containingClassOrObject?.isLocal == false
        else -> false
    }

    private fun findEnclosingNonLocalDeclaration(position: KtElement): KtNamedDeclaration? =
        position.parentsOfType<KtNamedDeclaration>().firstOrNull { ktDeclaration ->
            ktDeclaration.canBeEnclosingDeclaration()
        }

    private fun recordOriginalDeclaration(targetDeclaration: KtNamedDeclaration, originalDeclaration: KtNamedDeclaration) {
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
            replacement = RawFirReplacement(place, elementToResolve),
            onAirCreatedDeclaration = true
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
            withPsiEntry("place", place)
            withPsiEntry("elementToResolve", elementToResolve)
        }
    }

    fun onAirGetTowerContextProvider(
        firResolveSession: LLFirResolveSession,
        place: KtElement,
    ): FirTowerContextProvider {
        require(firResolveSession is LLFirResolvableResolveSession)

        return if (place is KtFile) {
            FileTowerProvider(place, onAirGetTowerContextForFile(firResolveSession, place))
        } else {
            val validPlace = PsiTreeUtil.findFirstParent(place, false) {
                RawFirReplacement.isApplicableForReplacement(it as KtElement)
            } as KtElement

            FirTowerDataContextAllElementsCollector().also {
                runBodyResolveOnAir(
                    firResolveSession = firResolveSession,
                    collector = it,
                    onAirCreatedDeclaration = false,
                    replacement = RawFirReplacement(validPlace, validPlace),
                )
            }
        }
    }

    private fun onAirGetTowerContextForFile(
        firResolveSession: LLFirResolvableResolveSession,
        file: KtFile,
    ): FirTowerDataContext {
        val session = firResolveSession.getSessionFor(file.getKtModule(firResolveSession.project)) as LLFirResolvableModuleSession
        val moduleComponents = session.moduleComponents

        val firFile = moduleComponents.firFileBuilder.buildRawFirFileWithCaching(file)

        val scopeSession = firResolveSession.getScopeSessionFor(session)
        moduleComponents.firModuleLazyDeclarationResolver.lazyResolveFileDeclaration(
            firFile = firFile,
            scopeSession = scopeSession,
            toPhase = FirResolvePhase.IMPORTS
        )

        val importingScopes = createImportingScopes(firFile, firFile.moduleData.session, scopeSession, useCaching = false)
        val fileScopeElements = importingScopes.map { it.asTowerDataElement(isLocal = false) }
        return FirTowerDataContext().addNonLocalTowerDataElements(fileScopeElements)
    }

    fun getFirResolveSessionForDependentCopy(
        originalFirResolveSession: LLFirResolveSession,
        originalKtFile: KtFile,
        elementToAnalyze: KtElement
    ): LLFirResolveSession {
        require(originalFirResolveSession is LLFirResolvableResolveSession)
        require(elementToAnalyze !is KtFile) { "KtFile for dependency element not supported" }

        val dependencyNonLocalDeclaration = findEnclosingNonLocalDeclaration(elementToAnalyze)
            ?: return LLFirResolveSessionDepended(
                originalFirResolveSession,
                FileTowerProvider(elementToAnalyze.containingKtFile, onAirGetTowerContextForFile(originalFirResolveSession, originalKtFile)),
                ktToFirMapping = null
            )


        val sameDeclarationInOriginalFile = PsiTreeUtil.findSameElementInCopy(dependencyNonLocalDeclaration, originalKtFile)
            ?: buildErrorWithAttachment("Cannot find original function matching") {
                withPsiEntry("matchingPsi", dependencyNonLocalDeclaration)
                withPsiEntry("originalFile", originalKtFile)
            }

        recordOriginalDeclaration(
            targetDeclaration = dependencyNonLocalDeclaration,
            originalDeclaration = sameDeclarationInOriginalFile
        )

        val collector = FirTowerDataContextAllElementsCollector()
        val copiedFirDeclaration = runBodyResolveOnAir(
            originalFirResolveSession,
            replacement = RawFirReplacement(sameDeclarationInOriginalFile, dependencyNonLocalDeclaration),
            onAirCreatedDeclaration = true,
            collector = collector,
        )

        val mapping = KtToFirMapping(copiedFirDeclaration, FirElementsRecorder())
        return LLFirResolveSessionDepended(originalFirResolveSession, collector, mapping)
    }

    private fun tryResolveAsFileAnnotation(
        annotationEntry: KtAnnotationEntry,
        replacement: RawFirReplacement,
        firFile: FirFile,
        collector: FirTowerDataContextCollector? = null,
    ): FirAnnotation {
        val annotationCall = buildFileFirAnnotation(
            session = firFile.moduleData.session,
            baseScopeProvider = firFile.moduleData.session.kotlinScopeProvider,
            fileAnnotation = annotationEntry,
            replacement = replacement
        )
        val llFirResolvableSession = firFile.llFirResolvableSession
            ?: buildErrorWithAttachment("FirFile session expected to be a resolvable session but was ${firFile.llFirSession::class.java}") {
                withEntry("firSession", firFile.llFirSession) { it.toString() }
            }
        val declarationResolver = llFirResolvableSession.moduleComponents.firModuleLazyDeclarationResolver

        declarationResolver.resolveFileAnnotations(
            firFile = firFile,
            annotations = listOf(annotationCall),
            scopeSession = ScopeSession(),
            checkPCE = true,
            collector = collector
        )

        return annotationCall
    }

    private fun runBodyResolveOnAir(
        firResolveSession: LLFirResolvableResolveSession,
        replacement: RawFirReplacement,
        onAirCreatedDeclaration: Boolean,
        collector: FirTowerDataContextCollector? = null,
    ): FirElement {
        val nonLocalDeclaration = findEnclosingNonLocalDeclaration(replacement.from)
        val originalFirFile = firResolveSession.getOrBuildFirFile(replacement.from.containingKtFile)

        if (nonLocalDeclaration == null) {
            //It is possible that it is file annotation is going to resolve
            val annotationCall = replacement.from.parentOfType<KtAnnotationEntry>(withSelf = true)
            if (annotationCall != null) {
                return tryResolveAsFileAnnotation(
                    annotationEntry = annotationCall,
                    replacement = replacement,
                    firFile = originalFirFile,
                    collector = collector,
                )
            } else {
                error("Cannot find enclosing declaration for ${replacement.from.getElementTextInContext()}")
            }
        }

        val originalDeclaration = nonLocalDeclaration.getOrBuildFirOfType<FirDeclaration>(firResolveSession)

        val originalDesignation = originalDeclaration.collectDesignation()

        val newDeclarationWithReplacement = RawFirNonLocalDeclarationBuilder.buildWithReplacement(
            session = originalDeclaration.moduleData.session,
            scopeProvider = originalDeclaration.moduleData.session.kotlinScopeProvider,
            designation = originalDesignation,
            rootNonLocalDeclaration = nonLocalDeclaration,
            replacement = replacement,
        )

        val isInBodyReplacement = isInBodyReplacement(nonLocalDeclaration, replacement)

        return firResolveSession.globalComponents.lockProvider.runCustomResolveUnderLock(originalFirFile, true) {
            val copiedFirDeclaration = if (isInBodyReplacement) {
                when (originalDeclaration) {
                    is FirSimpleFunction ->
                        originalDeclaration.withBodyFrom(newDeclarationWithReplacement as FirSimpleFunction)
                    is FirProperty ->
                        originalDeclaration.withBodyFrom(newDeclarationWithReplacement as FirProperty)
                    is FirRegularClass ->
                        originalDeclaration.withBodyFrom(newDeclarationWithReplacement as FirRegularClass)
                    is FirTypeAlias -> newDeclarationWithReplacement
                    else -> error("Not supported type ${originalDeclaration::class.simpleName}")
                }
            } else newDeclarationWithReplacement

            val onAirDesignation = FirDeclarationDesignationWithFile(
                path = originalDesignation.path,
                declaration = copiedFirDeclaration,
                firFile = originalFirFile
            )
            ResolveTreeBuilder.resolveEnsure(onAirDesignation.declaration, FirResolvePhase.BODY_RESOLVE) {
                val resolvableSession = onAirDesignation.declaration.llFirResolvableSession
                    ?: error("Expected resolvable session")
                resolvableSession.moduleComponents.firModuleLazyDeclarationResolver
                    .runLazyDesignatedOnAirResolveToBodyWithoutLock(
                        designation = onAirDesignation,
                        checkPCE = true,
                        onAirCreatedDeclaration = onAirCreatedDeclaration,
                        towerDataContextCollector = collector,
                    )
            }
            copiedFirDeclaration
        }

    }

    private fun isInBodyReplacement(ktDeclaration: KtDeclaration, replacement: RawFirReplacement): Boolean = when (ktDeclaration) {
        is KtNamedFunction ->
            ktDeclaration.bodyBlockExpression?.let { it.isAncestor(replacement.from, true) } ?: false
        is KtProperty -> {
            val insideGetterBody = ktDeclaration.getter?.bodyBlockExpression?.let {
                it.isAncestor(replacement.from, true)
            } ?: false

            val insideGetterOrSetterBody = insideGetterBody || ktDeclaration.setter?.bodyBlockExpression?.let {
                it.isAncestor(replacement.from, true)
            } ?: false

            insideGetterOrSetterBody || ktDeclaration.initializer?.let {
                it.isAncestor(replacement.from, true)
            } ?: false
        }
        is KtClassOrObject ->
            ktDeclaration.body?.let { it.isAncestor(replacement.from, true) } ?: false
        is KtTypeAlias -> false
        else -> error("Not supported type ${ktDeclaration::class.simpleName}")
    }

    fun onAirResolveTypeInPlace(
        place: KtElement,
        typeReference: KtTypeReference,
        firResolveSession: LLFirResolveSession,
    ): FirResolvedTypeRef {
        val context = firResolveSession.getTowerContextProvider(place.containingKtFile).getClosestAvailableParentContext(place)
            ?: error("TowerContext not found for ${place.getElementTextInContext()}")

        val session = firResolveSession.useSiteFirSession
        val firTypeReference = buildFirUserTypeRef(
            typeReference = typeReference,
            session = session,
            baseScopeProvider = session.kotlinScopeProvider
        )

        return FirTypeResolveTransformer(
            session = session,
            scopeSession = ScopeSession(),
            initialScopes = context.towerDataElements.asReversed().mapNotNull { it.scope }
        ).transformTypeRef(firTypeReference, null)
    }
}
