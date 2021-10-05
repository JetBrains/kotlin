/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.api

import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.analysis.api.impl.barebone.parentOfType
import org.jetbrains.kotlin.analysis.api.impl.barebone.parentsOfType
import org.jetbrains.kotlin.analysis.low.level.api.fir.DeclarationCopyBuilder.withBodyFrom
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.realPsi
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.transformers.FirTypeResolveTransformer
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.FirTowerDataContextCollector
import org.jetbrains.kotlin.fir.scopes.createImportingScopes
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.fir.visitors.FirVisitorVoid
import org.jetbrains.kotlin.analysis.low.level.api.fir.FirModuleResolveStateDepended
import org.jetbrains.kotlin.analysis.low.level.api.fir.FirModuleResolveStateImpl
import org.jetbrains.kotlin.analysis.low.level.api.fir.element.builder.FileTowerProvider
import org.jetbrains.kotlin.analysis.low.level.api.fir.element.builder.FirTowerContextProvider
import org.jetbrains.kotlin.analysis.low.level.api.fir.element.builder.FirTowerDataContextAllElementsCollector
import org.jetbrains.kotlin.analysis.low.level.api.fir.file.builder.runCustomResolveUnderLock
import org.jetbrains.kotlin.analysis.low.level.api.fir.file.structure.FirElementsRecorder
import org.jetbrains.kotlin.analysis.low.level.api.fir.file.structure.KtToFirMapping
import org.jetbrains.kotlin.analysis.low.level.api.fir.lazy.resolve.*
import org.jetbrains.kotlin.analysis.low.level.api.fir.providers.firIdeProvider
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.FirIdeSourcesSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.getElementTextInContext
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.originalDeclaration
import org.jetbrains.kotlin.analysis.project.structure.getKtModule
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.containingClassOrObject
import org.jetbrains.kotlin.psi.psiUtil.isAncestor

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
        require(!targetDeclaration.isPhysical)
        require(originalDeclaration.containingKtFile !== targetDeclaration.containingKtFile)
        val originalDeclarationParents = originalDeclaration.parentsOfType<KtDeclaration>().toList()
        val fakeDeclarationParents = targetDeclaration.parentsOfType<KtDeclaration>().toList()
        originalDeclarationParents.zip(fakeDeclarationParents) { original, fake ->
            fake.originalDeclaration = original
        }
    }

    fun <T : KtElement> onAirResolveElement(
        state: FirModuleResolveState,
        place: T,
        elementToResolve: T,
    ): FirElement {
        require(state is FirModuleResolveStateImpl)
        require(place.isPhysical)

        val declaration = runBodyResolveOnAir(
            state = state,
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
        return expressionLocator.result ?: error("Resolved on-air element was not found in containing declaration")
    }

    fun onAirGetTowerContextProvider(
        state: FirModuleResolveState,
        place: KtElement,
    ): FirTowerContextProvider {
        require(state is FirModuleResolveStateImpl)

        return if (place is KtFile) {
            FileTowerProvider(place, onAirGetTowerContextForFile(state, place))
        } else {
            val validPlace = PsiTreeUtil.findFirstParent(place, false) {
                RawFirReplacement.isApplicableForReplacement(it as KtElement)
            } as KtElement

            FirTowerDataContextAllElementsCollector().also {
                runBodyResolveOnAir(
                    state = state,
                    collector = it,
                    onAirCreatedDeclaration = false,
                    replacement = RawFirReplacement(validPlace, validPlace),
                )
            }
        }
    }

    private fun onAirGetTowerContextForFile(
        state: FirModuleResolveStateImpl,
        file: KtFile,
    ): FirTowerDataContext {
        require(file.isPhysical)
        val session = state.getSessionFor(file.getKtModule(state.project)) as FirIdeSourcesSession

        val firFile = session.firFileBuilder.buildRawFirFileWithCaching(
            ktFile = file,
            cache = session.cache,
            preferLazyBodies = true
        )

        state.firLazyDeclarationResolver.lazyResolveFileDeclaration(
            firFile = firFile,
            moduleFileCache = session.cache,
            scopeSession = ScopeSession(),
            toPhase = FirResolvePhase.IMPORTS
        )

        val importingScopes = createImportingScopes(firFile, firFile.moduleData.session, ScopeSession(), useCaching = false)
        val fileScopeElements = importingScopes.map { it.asTowerDataElement(isLocal = false) }
        return FirTowerDataContext().addNonLocalTowerDataElements(fileScopeElements)
    }

    fun getResolveStateForDependentCopy(
        originalState: FirModuleResolveState,
        originalKtFile: KtFile,
        elementToAnalyze: KtElement
    ): FirModuleResolveState {
        require(originalState is FirModuleResolveStateImpl)
        require(elementToAnalyze !is KtFile) { "KtFile for dependency element not supported" }
        require(!elementToAnalyze.isPhysical) { "Depended state should be build only for non-physical elements" }

        val dependencyNonLocalDeclaration = findEnclosingNonLocalDeclaration(elementToAnalyze)
            ?: return FirModuleResolveStateDepended(
                originalState,
                FileTowerProvider(elementToAnalyze.containingKtFile, onAirGetTowerContextForFile(originalState, originalKtFile)),
                ktToFirMapping = null
            )


        val sameDeclarationInOriginalFile = PsiTreeUtil.findSameElementInCopy(dependencyNonLocalDeclaration, originalKtFile)
            ?: error("Cannot find original function matching to ${dependencyNonLocalDeclaration.getElementTextInContext()} in $originalKtFile")

        recordOriginalDeclaration(
            targetDeclaration = dependencyNonLocalDeclaration,
            originalDeclaration = sameDeclarationInOriginalFile
        )

        val collector = FirTowerDataContextAllElementsCollector()
        val copiedFirDeclaration = runBodyResolveOnAir(
            originalState,
            replacement = RawFirReplacement(sameDeclarationInOriginalFile, dependencyNonLocalDeclaration),
            onAirCreatedDeclaration = true,
            collector = collector,
        )

        val mapping = KtToFirMapping(copiedFirDeclaration, FirElementsRecorder())
        return FirModuleResolveStateDepended(originalState, collector, mapping)
    }

    private fun tryResolveAsFileAnnotation(
        annotationEntry: KtAnnotationEntry,
        state: FirModuleResolveStateImpl,
        replacement: RawFirReplacement,
        firFile: FirFile,
        collector: FirTowerDataContextCollector? = null,
    ): FirAnnotation {
        val annotationCall = buildFileFirAnnotation(
            session = firFile.moduleData.session,
            baseScopeProvider = firFile.moduleData.session.firIdeProvider.kotlinScopeProvider,
            fileAnnotation = annotationEntry,
            replacement = replacement
        )
        state.firLazyDeclarationResolver.resolveFileAnnotations(
            firFile = firFile,
            annotations = listOf(annotationCall),
            moduleFileCache = state.rootModuleSession.cache,
            scopeSession = ScopeSession(),
            checkPCE = true,
            collector = collector
        )

        return annotationCall
    }

    private fun runBodyResolveOnAir(
        state: FirModuleResolveStateImpl,
        replacement: RawFirReplacement,
        onAirCreatedDeclaration: Boolean,
        collector: FirTowerDataContextCollector? = null,
    ): FirElement {

        val nonLocalDeclaration = findEnclosingNonLocalDeclaration(replacement.from)
        val originalFirFile = state.getOrBuildFirFile(replacement.from.containingKtFile)

        if (nonLocalDeclaration == null) {
            //It is possible that it is file annotation is going to resolve
            val annotationCall = replacement.from.parentOfType<KtAnnotationEntry>(withSelf = true)
            if (annotationCall != null) {
                return tryResolveAsFileAnnotation(
                    annotationEntry = annotationCall,
                    state = state,
                    replacement = replacement,
                    firFile = originalFirFile,
                    collector = collector,
                )
            } else {
                error("Cannot find enclosing declaration for ${replacement.from.getElementTextInContext()}")
            }
        }

        val originalDeclaration = nonLocalDeclaration.getOrBuildFirOfType<FirDeclaration>(state)

        val originalDesignation = originalDeclaration.collectDesignation()

        val newDeclarationWithReplacement = RawFirNonLocalDeclarationBuilder.buildWithReplacement(
            session = originalDeclaration.moduleData.session,
            scopeProvider = originalDeclaration.moduleData.session.firIdeProvider.kotlinScopeProvider,
            designation = originalDesignation,
            rootNonLocalDeclaration = nonLocalDeclaration,
            replacement = replacement,
        )

        val isInBodyReplacement = isInBodyReplacement(nonLocalDeclaration, replacement)

        return state.rootModuleSession.cache.firFileLockProvider.runCustomResolveUnderLock(originalFirFile, true) {
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
                state.firLazyDeclarationResolver.runLazyDesignatedOnAirResolveToBodyWithoutLock(
                    designation = onAirDesignation,
                    moduleFileCache = state.rootModuleSession.cache,
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
        state: FirModuleResolveState
    ): FirResolvedTypeRef {
        val context = state.getTowerContextProvider().getClosestAvailableParentContext(place)
            ?: error("TowerContext not found for ${place.getElementTextInContext()}")

        val session = state.rootModuleSession
        val firTypeReference = buildFirUserTypeRef(
            typeReference = typeReference,
            session = session,
            baseScopeProvider = session.firIdeProvider.kotlinScopeProvider
        )

        return FirTypeResolveTransformer(
            session = session,
            scopeSession = ScopeSession(),
            initialScopes = context.towerDataElements.asReversed().mapNotNull { it.scope }
        ).transformTypeRef(firTypeReference, null)
    }

    private class TowerProviderForElementForState(private val state: FirModuleResolveState) : FirTowerContextProvider {
        override fun getClosestAvailableParentContext(ktElement: KtElement): FirTowerDataContext? {
            return if (state is FirModuleResolveStateDepended) {
                state.towerProviderBuiltUponElement.getClosestAvailableParentContext(ktElement)
                    ?: onAirGetTowerContextProvider(state.originalState, ktElement).getClosestAvailableParentContext(ktElement)
            } else {
                onAirGetTowerContextProvider(state, ktElement).getClosestAvailableParentContext(ktElement)
            }
        }
    }

    fun FirModuleResolveState.getTowerContextProvider(): FirTowerContextProvider =
        TowerProviderForElementForState(this)
}
