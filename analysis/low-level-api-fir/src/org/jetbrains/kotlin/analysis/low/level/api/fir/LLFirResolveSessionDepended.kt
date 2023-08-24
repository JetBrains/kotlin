/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir

import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootModificationTracker
import com.intellij.psi.util.PsiModificationTracker
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.DiagnosticCheckerFilter
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.LLFirResolveSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.element.builder.FirElementBuilder
import org.jetbrains.kotlin.analysis.low.level.api.fir.element.builder.FirTowerContextProvider
import org.jetbrains.kotlin.analysis.low.level.api.fir.file.structure.KtToFirMapping
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.LLFirSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.state.LLFirResolvableResolveSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.state.TowerProviderForElementForState
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.LLFirScopeSessionProvider
import org.jetbrains.kotlin.analysis.project.structure.KtModule
import org.jetbrains.kotlin.analysis.utils.caches.*
import org.jetbrains.kotlin.diagnostics.KtPsiDiagnostic
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.originalDeclaration

/**
 * An [LLFirResolveSession] which *depends* on an [originalFirResolveSession], but can provide its own FIR elements and symbols selectively.
 * The dependent session mostly provides FIR elements and symbols from the original resolve session, but additionally provides FIR elements
 * (and their associated symbols) from its own [ktToFirMapping].
 *
 * [LLFirResolveSessionDepended] is used by on-air resolve to provide FIR elements for a copied declaration in the larger context of the
 * original resolve session.
 */
internal class LLFirResolveSessionDepended(
    val originalFirResolveSession: LLFirResolvableResolveSession,
    val towerProviderBuiltUponElement: FirTowerContextProvider,
    private val ktToFirMapping: KtToFirMapping?,
) : LLFirResolveSession() {
    override val project: Project get() = originalFirResolveSession.project
    override val useSiteKtModule: KtModule get() = originalFirResolveSession.useSiteKtModule
    override val useSiteFirSession get() = originalFirResolveSession.useSiteFirSession

    private val scopeSessionProviderCache = SoftCachedMap.create<FirSession, LLFirScopeSessionProvider>(
        project,
        SoftCachedMap.Kind.SOFT_KEYS_SOFT_VALUES,
        listOf(
            PsiModificationTracker.MODIFICATION_COUNT,
            ProjectRootModificationTracker.getInstance(project)
        )
    )

    override fun getScopeSessionFor(firSession: FirSession): ScopeSession {
        return scopeSessionProviderCache
            .getOrPut(firSession) { LLFirScopeSessionProvider.create(project, invalidationTrackers = emptyList()) }
            .getScopeSession()
    }

    override fun getSessionFor(module: KtModule): LLFirSession =
        originalFirResolveSession.getSessionFor(module)

    override fun getOrBuildFirFor(element: KtElement): FirElement? {
        val psi = FirElementBuilder.getPsiAsFirElementSource(element) ?: return null
        ktToFirMapping?.getFir(psi)?.let { return it }
        return originalFirResolveSession.getOrBuildFirFor(element = element)
    }

    override fun getOrBuildFirFile(ktFile: KtFile): FirFile =
        originalFirResolveSession.getOrBuildFirFile(ktFile)

    override fun resolveFirToPhase(declaration: FirDeclaration, toPhase: FirResolvePhase) {
        originalFirResolveSession.resolveFirToPhase(declaration, toPhase)
    }

    override fun getDiagnostics(element: KtElement, filter: DiagnosticCheckerFilter): List<KtPsiDiagnostic> =
        TODO("Diagnostics are not implemented for depended state")

    override fun collectDiagnosticsForFile(ktFile: KtFile, filter: DiagnosticCheckerFilter): Collection<KtPsiDiagnostic> =
        TODO("Diagnostics are not implemented for depended state")

    override fun resolveToFirSymbol(ktDeclaration: KtDeclaration, phase: FirResolvePhase): FirBasedSymbol<*> {
        val declarationToResolve = ktDeclaration.originalDeclaration ?: ktDeclaration
        ktToFirMapping?.getElement(declarationToResolve)?.let { it as? FirDeclaration }?.symbol?.let { return it }

        return originalFirResolveSession.resolveToFirSymbol(declarationToResolve, phase)
    }

    override fun getTowerContextProvider(ktFile: KtFile): FirTowerContextProvider =
        TowerProviderForElementForState(this)
}
