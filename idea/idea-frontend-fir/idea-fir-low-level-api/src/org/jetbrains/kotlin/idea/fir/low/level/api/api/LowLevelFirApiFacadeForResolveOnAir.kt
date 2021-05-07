/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api.api

import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.parentsOfType
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.builder.RawFirFragmentForLazyBodiesBuilder
import org.jetbrains.kotlin.fir.builder.RawFirReplacement
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.realPsi
import org.jetbrains.kotlin.fir.resolve.*
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.FirTowerDataContextCollector
import org.jetbrains.kotlin.fir.scopes.createImportingScopes
import org.jetbrains.kotlin.fir.visitors.FirVisitorVoid
import org.jetbrains.kotlin.idea.caches.project.getModuleInfo
import org.jetbrains.kotlin.idea.fir.low.level.api.FirModuleResolveStateDepended
import org.jetbrains.kotlin.idea.fir.low.level.api.FirModuleResolveStateImpl
import org.jetbrains.kotlin.idea.fir.low.level.api.element.builder.FirTowerContextProvider
import org.jetbrains.kotlin.idea.fir.low.level.api.element.builder.FirTowerDataContextAllElementsCollector
import org.jetbrains.kotlin.idea.fir.low.level.api.element.builder.FileTowerProvider
import org.jetbrains.kotlin.idea.fir.low.level.api.file.structure.FirElementsRecorder
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

    private fun <T : KtElement> locateDeclarationInFileByOffset(offsetElement: T, file: KtFile): T? {
        val elementOffset = offsetElement.textOffset
        val elementAtOffset = file.findElementAt(elementOffset) ?: return null
        return PsiTreeUtil.getParentOfType(elementAtOffset, offsetElement::class.java, false)?.takeIf { it.textOffset == elementOffset }
    }

    private fun recordOriginalDeclaration(targetDeclaration: KtNamedDeclaration, originalDeclaration: KtNamedDeclaration) {
        require(!targetDeclaration.isPhysical)
        require(originalDeclaration.containingKtFile !== targetDeclaration.containingKtFile)
        val originalDeclrationParents = originalDeclaration.parentsOfType<KtDeclaration>().toList()
        val fakeDeclarationParents = targetDeclaration.parentsOfType<KtDeclaration>().toList()
        originalDeclrationParents.zip(fakeDeclarationParents) { original, fake ->
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
                RawFirFragmentForLazyBodiesBuilder.elementIsApplicable(it as KtElement)
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

        val importingScopes = createImportingScopes(firFile, firFile.declarationSiteSession, ScopeSession(), useCaching = false)
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
                emptyMap()
            )


        val sameDeclarationInOriginalFile = locateDeclarationInFileByOffset(dependencyNonLocalDeclaration, originalKtFile)
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

        val recordedMap = FirElementsRecorder.recordElementsFrom(copiedFirDeclaration, FirElementsRecorder())
        return FirModuleResolveStateDepended(originalState, collector, recordedMap)
    }

    private fun <T : KtElement> runBodyResolveOnAir(
        state: FirModuleResolveStateImpl,
        replacement: RawFirReplacement<T>,
        isOnAirResolve: Boolean,
        collector: FirTowerDataContextCollector? = null,
    ): FirDeclaration {

        val nonLocalDeclaration = findEnclosingNonLocalDeclaration(replacement.from)
            ?: error("Cannot find enclosing declaration for ${replacement.from.getElementTextInContext()}")

        val copiedFirDeclaration = DeclarationCopyBuilder.createDeclarationCopy(
            state = state,
            nonLocalDeclaration = nonLocalDeclaration,
            replacement = replacement,
        )

        val originalFirFile = state.getOrBuildFirFile(nonLocalDeclaration.containingKtFile)

        state.firLazyDeclarationResolver.lazyDesignatedResolveDeclaration(
            firDeclarationToResolve = copiedFirDeclaration,
            moduleFileCache = state.rootModuleSession.cache,
            containerFirFile = originalFirFile,
            provider = originalFirFile.declarationSiteSession.firIdeProvider,
            toPhase = FirResolvePhase.BODY_RESOLVE,
            checkPCE = true,
            isOnAirResolve = isOnAirResolve,
            towerDataContextCollector = collector,
        )

        return copiedFirDeclaration
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
