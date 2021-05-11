/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api.api

import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.parentOfType
import com.intellij.psi.util.parentsOfType
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.builder.RawFirBuilder
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.extensions.predicateBasedProvider
import org.jetbrains.kotlin.fir.realPsi
import org.jetbrains.kotlin.fir.resolve.FirTowerDataContext
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.asTowerDataElement
import org.jetbrains.kotlin.fir.resolve.transformers.FirTypeResolveTransformer
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.FirTowerDataContextCollector
import org.jetbrains.kotlin.fir.resolve.typeResolver
import org.jetbrains.kotlin.fir.scopes.FirCompositeScope
import org.jetbrains.kotlin.fir.scopes.createImportingScopes
import org.jetbrains.kotlin.fir.toFirPsiSourceElement
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.types.toFirResolvedTypeRef
import org.jetbrains.kotlin.fir.visitors.FirVisitorVoid
import org.jetbrains.kotlin.idea.caches.project.getModuleInfo
import org.jetbrains.kotlin.idea.fir.low.level.api.FirModuleResolveStateDepended
import org.jetbrains.kotlin.idea.fir.low.level.api.FirModuleResolveStateImpl
import org.jetbrains.kotlin.idea.fir.low.level.api.element.builder.FileTowerProvider
import org.jetbrains.kotlin.idea.fir.low.level.api.element.builder.FirTowerContextProvider
import org.jetbrains.kotlin.idea.fir.low.level.api.element.builder.FirTowerDataContextAllElementsCollector
import org.jetbrains.kotlin.idea.fir.low.level.api.file.structure.FirElementsRecorder
import org.jetbrains.kotlin.idea.fir.low.level.api.file.structure.KtToFirMapping
import org.jetbrains.kotlin.idea.fir.low.level.api.lazy.resolve.RawFirReplacement
import org.jetbrains.kotlin.idea.fir.low.level.api.lazy.resolve.buildFileFirAnnotation
import org.jetbrains.kotlin.idea.fir.low.level.api.lazy.resolve.buildFirUserTypeRef
import org.jetbrains.kotlin.idea.fir.low.level.api.providers.firIdeProvider
import org.jetbrains.kotlin.idea.fir.low.level.api.sessions.FirIdeSourcesSession
import org.jetbrains.kotlin.idea.fir.low.level.api.util.originalDeclaration
import org.jetbrains.kotlin.idea.util.getElementTextInContext
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.containingClassOrObject

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
            isOnAirResolve = true,
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
        require(place.isPhysical)

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
                    replacement = RawFirReplacement(validPlace, validPlace),
                    isOnAirResolve = false //isOnAirResolve can be little faster because node resolved in it's context
                )
            }
        }
    }

    private fun onAirGetTowerContextForFile(
        state: FirModuleResolveState,
        file: KtFile,
    ): FirTowerDataContext {
        require(file.isPhysical)
        val session = state.getSessionFor(file.getModuleInfo()) as FirIdeSourcesSession
        val firFile = session.firFileBuilder.getFirFileResolvedToPhaseWithCaching(
            file,
            session.cache,
            FirResolvePhase.IMPORTS,
            ScopeSession(),
            checkPCE = false
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
            isOnAirResolve = true,
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
    ): FirAnnotationCall {
        val annotationCall = buildFileFirAnnotation(
            firFile.moduleData.session,
            firFile.moduleData.session.firIdeProvider.kotlinScopeProvider,
            annotationEntry,
            replacement
        )
        state.firLazyDeclarationResolver.resolveFileAnnotations(
            firFile,
            listOf(annotationCall),
            state.rootModuleSession.cache,
            ScopeSession(),
            collector
        )

        return annotationCall
    }

    private fun runBodyResolveOnAir(
        state: FirModuleResolveStateImpl,
        replacement: RawFirReplacement,
        isOnAirResolve: Boolean,
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

        val copiedFirDeclaration = DeclarationCopyBuilder.createDeclarationCopy(
            state = state,
            nonLocalDeclaration = nonLocalDeclaration,
            replacement = replacement,
        )

        state.firLazyDeclarationResolver.lazyDesignatedResolveDeclaration(
            firDeclarationToResolve = copiedFirDeclaration,
            moduleFileCache = state.rootModuleSession.cache,
            containerFirFile = originalFirFile,
            provider = originalFirFile.moduleData.session.firIdeProvider,
            toPhase = FirResolvePhase.BODY_RESOLVE,
            checkPCE = true,
            isOnAirResolve = isOnAirResolve,
            towerDataContextCollector = collector,
        )

        return copiedFirDeclaration
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
            return if (ktElement.isPhysical) {
                onAirGetTowerContextProvider(state, ktElement).getClosestAvailableParentContext(ktElement)
            } else {
                require(state is FirModuleResolveStateDepended) {
                    "Invalid resolve state ${this::class.simpleName} but have to be ${FirModuleResolveStateDepended::class.simpleName}"
                }
                state.towerProviderBuiltUponElement.getClosestAvailableParentContext(ktElement)
            }
        }
    }

    fun FirModuleResolveState.getTowerContextProvider(): FirTowerContextProvider =
        TowerProviderForElementForState(this)
}
