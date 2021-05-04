/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api.api

import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.parentsOfType
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.builder.RawFirReplacement
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.realPsi
import org.jetbrains.kotlin.fir.resolve.FirTowerDataContext
import org.jetbrains.kotlin.fir.resolve.ResolutionMode
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.FirBodyResolveTransformer
import org.jetbrains.kotlin.fir.visitors.FirVisitorVoid
import org.jetbrains.kotlin.idea.fir.low.level.api.FirModuleResolveStateDepended
import org.jetbrains.kotlin.idea.fir.low.level.api.FirModuleResolveStateImpl
import org.jetbrains.kotlin.idea.fir.low.level.api.element.builder.FirTowerContextProvider
import org.jetbrains.kotlin.idea.fir.low.level.api.element.builder.FirTowerDataContextCollector
import org.jetbrains.kotlin.idea.fir.low.level.api.element.builder.FirTowerDataContextAllElementsCollector
import org.jetbrains.kotlin.idea.fir.low.level.api.element.builder.SingleElementTowerProvider
import org.jetbrains.kotlin.idea.fir.low.level.api.file.structure.FirElementsRecorder
import org.jetbrains.kotlin.idea.fir.low.level.api.providers.firIdeProvider
import org.jetbrains.kotlin.idea.fir.low.level.api.util.originalDeclaration
import org.jetbrains.kotlin.idea.util.getElementTextInContext
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.containingClassOrObject

object LowLevelFirApiFacadeForDependentCopy {

    private fun KtDeclaration.canBeEnclosingDeclaration(): Boolean = when (this) {
        is KtNamedFunction -> isTopLevel || containingClassOrObject?.isLocal == false
        is KtProperty -> isTopLevel || containingClassOrObject?.isLocal == false
        is KtClassOrObject -> !isLocal
        is KtTypeAlias -> isTopLevel() || containingClassOrObject?.isLocal == false
        else -> false
    }

    fun findEnclosingNonLocalDeclaration(position: KtElement): KtNamedDeclaration? =
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

    private fun <T : KtElement> onAirResolveElement(
        state: FirModuleResolveState,
        place: T,
        elementToResolve: T,
    ): FirModuleResolveState {
        require(state is FirModuleResolveStateImpl)
        require(place.isPhysical)
        require(!elementToResolve.isPhysical)

        val collector = FirTowerDataContextAllElementsCollector()
        val declaration = runBodyResolve(state, replacement = RawFirReplacement(place, elementToResolve))

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

        val recordedMap = FirElementsRecorder.recordElementsFrom(declaration, FirElementsRecorder())
        return FirModuleResolveStateDepended(state, collector, recordedMap)
    }

    fun onAirGetTowerContextProvider(
        state: FirModuleResolveState,
        place: KtElement,
    ): FirTowerContextProvider {
        require(state is FirModuleResolveStateImpl)
        require(place.isPhysical)

        return if (place is KtFile) {
            onAirGetTowerContextForFile(state, place)
        } else {
            FirTowerDataContextAllElementsCollector().also {
                runBodyResolve(state, collector = it, replacement = RawFirReplacement(place, place))
            }
        }
    }

    private fun onAirGetTowerContextForFile(
        state: FirModuleResolveState,
        file: KtFile,
    ): FirTowerContextProvider {

        val firFile = state.getOrBuildFirFor(file) as FirFile

        val fileTransformer = object : FirBodyResolveTransformer(
            session = firFile.declarationSiteSession,
            phase = FirResolvePhase.IMPLICIT_TYPES_BODY_RESOLVE,
            implicitTypeOnly = true,
            scopeSession = ScopeSession()
        ) {
            var result: FirTowerDataContext? = null
                private set

            override fun transformDeclarationContent(declaration: FirDeclaration, data: ResolutionMode): FirDeclaration {
                check(declaration is FirFile)
                result = context.towerDataContext
                return declaration
            }
        }
        firFile.transform<FirFile, ResolutionMode>(fileTransformer, ResolutionMode.ContextDependent)

        val fileContext = fileTransformer.result
        check(fileContext != null) { "File context not found for physical file" }

        return SingleElementTowerProvider(file, fileContext)
    }

    fun getResolveStateForDependedCopy(
        originalState: FirModuleResolveState,
        originalKtFile: KtFile,
        dependencyKtElement: KtElement
    ): FirModuleResolveState {
        require(originalState is FirModuleResolveStateImpl)
        require(dependencyKtElement !is KtFile) { "KtFile for dependency element not supported" }
        require(!dependencyKtElement.isPhysical) { "Depended state should be build only for non-physical elements" }

        val dependencyNonLocalDeclaration = findEnclosingNonLocalDeclaration(dependencyKtElement)
            ?: error("Cannot find enclosing declaration for ${dependencyKtElement.getElementTextInContext()}")

        val sameDeclarationInOriginalFile = locateDeclarationInFileByOffset(dependencyNonLocalDeclaration, originalKtFile)
            ?: error("Cannot find original function matching to ${dependencyNonLocalDeclaration.getElementTextInContext()} in $originalKtFile")

        recordOriginalDeclaration(
            targetDeclaration = dependencyNonLocalDeclaration,
            originalDeclaration = sameDeclarationInOriginalFile
        )

        val collector = FirTowerDataContextAllElementsCollector()
        val copiedFirDeclaration = runBodyResolve(
            originalState,
            collector = collector,
            replacement = RawFirReplacement(sameDeclarationInOriginalFile, dependencyNonLocalDeclaration)
        )

        val recordedMap = FirElementsRecorder.recordElementsFrom(copiedFirDeclaration, FirElementsRecorder())
        return FirModuleResolveStateDepended(originalState, collector, recordedMap)
    }

    private fun <T : KtElement> runBodyResolve(
        state: FirModuleResolveStateImpl,
        replacement: RawFirReplacement<T>,
        collector: FirTowerDataContextCollector? = null,
    ): FirDeclaration {
        val copiedFirDeclaration = DeclarationCopyBuilder.createDeclarationCopy(
            state = state,
            replacement = replacement,
        )

        val originalFirFile = state.getOrBuildFirFor(replacement.from.containingKtFile) as FirFile

        state.firFileBuilder.runCustomResolveWithPCECheck(originalFirFile, state.rootModuleSession.cache) {
            state.firLazyDeclarationResolver.runLazyResolveWithoutLock(
                copiedFirDeclaration,
                state.rootModuleSession.cache,
                originalFirFile,
                originalFirFile.declarationSiteSession.firIdeProvider,
                fromPhase = copiedFirDeclaration.resolvePhase,
                toPhase = FirResolvePhase.BODY_RESOLVE,
                towerDataContextCollector = collector,
                checkPCE = true,
                lastNonLazyPhase = FirResolvePhase.IMPORTS
            )
        }

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
