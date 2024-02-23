/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.api

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.LLFirSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.state.LLDiagnosticProvider
import org.jetbrains.kotlin.analysis.low.level.api.fir.state.LLModuleProvider
import org.jetbrains.kotlin.analysis.low.level.api.fir.state.LLModuleResolutionStrategyProvider
import org.jetbrains.kotlin.analysis.low.level.api.fir.state.LLScopeSessionProvider
import org.jetbrains.kotlin.analysis.low.level.api.fir.state.LLSessionProvider
import org.jetbrains.kotlin.analysis.project.structure.KtModule
import org.jetbrains.kotlin.analysis.project.structure.ProjectStructureProvider
import org.jetbrains.kotlin.analysis.utils.errors.requireIsInstance
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

/**
 * An entry point for a FIR Low Level API resolution. Represents a project view from a use-site [KtModule].
 */
abstract class LLFirResolveSession(
    val moduleProvider: LLModuleProvider,
    val resolutionStrategyProvider: LLModuleResolutionStrategyProvider,
    val sessionProvider: LLSessionProvider,
    val scopeSessionProvider: LLScopeSessionProvider,
    val diagnosticProvider: LLDiagnosticProvider
) {
    val useSiteKtModule: KtModule
        get() = moduleProvider.useSiteModule

    val project: Project
        get() = useSiteKtModule.project

    val useSiteFirSession: LLFirSession
        get() = sessionProvider.useSiteSession

    fun getSessionFor(module: KtModule): LLFirSession {
        return sessionProvider.getSession(module)
    }

    fun getScopeSessionFor(firSession: FirSession): ScopeSession {
        requireIsInstance<LLFirSession>(firSession)
        return scopeSessionProvider.getScopeSession(firSession)
    }

    /**
     * Build [FirElement] node in its final resolved state for a requested element.
     *
     * Note: that it isn't always [BODY_RESOLVE][FirResolvePhase.BODY_RESOLVE]
     * as not all declarations have types/bodies/etc. to resolve.
     *
     * This operation could be time-consuming because it creates
     * [FileStructureElement][org.jetbrains.kotlin.analysis.low.level.api.fir.file.structure.FileStructureElement]
     * and may resolve non-local declarations into [BODY_RESOLVE][FirResolvePhase.BODY_RESOLVE] phase.
     *
     * Please use [getOrBuildFirFile] to get [FirFile] in undefined phase.
     *
     * @return associated [FirElement] in final resolved state if it exists.
     *
     * @see getOrBuildFirFile
     * @see org.jetbrains.kotlin.analysis.low.level.api.fir.element.builder.FirElementBuilder.getOrBuildFirFor
     */
    internal abstract fun getOrBuildFirFor(element: KtElement): FirElement?

    /**
     * Get or build or get cached [FirFile] for requested file in undefined phase
     */
    internal abstract fun getOrBuildFirFile(ktFile: KtFile): FirFile

    /**
     * @see LLDiagnosticProvider.getDiagnostics
     */
    internal fun getDiagnostics(element: KtElement, filter: DiagnosticCheckerFilter): List<KtPsiDiagnostic> {
        return diagnosticProvider.getDiagnostics(element, filter)
    }

    /**
     * @see LLDiagnosticProvider.collectDiagnostics
     */
    internal fun collectDiagnosticsForFile(ktFile: KtFile, filter: DiagnosticCheckerFilter): Collection<KtPsiDiagnostic> {
        return diagnosticProvider.collectDiagnostics(ktFile, filter)
    }

    abstract fun resolveToFirSymbol(ktDeclaration: KtDeclaration, phase: FirResolvePhase): FirBasedSymbol<*>

    internal abstract fun resolveFirToPhase(declaration: FirDeclaration, toPhase: FirResolvePhase)
}

fun LLFirResolveSession.getModule(element: PsiElement): KtModule {
    return ProjectStructureProvider.getModule(project, element, useSiteKtModule)
}