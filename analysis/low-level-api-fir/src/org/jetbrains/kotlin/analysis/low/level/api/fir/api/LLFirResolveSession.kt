/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.api

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.low.level.api.fir.element.builder.FirTowerContextProvider
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.LLFirSession
import org.jetbrains.kotlin.analysis.project.structure.KtModule
import org.jetbrains.kotlin.analysis.project.structure.ProjectStructureProvider
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
abstract class LLFirResolveSession {
    abstract val project: Project

    abstract val useSiteFirSession: LLFirSession

    abstract val useSiteKtModule: KtModule

    abstract fun getSessionFor(module: KtModule): LLFirSession

    abstract fun getScopeSessionFor(firSession: FirSession): ScopeSession

    /**
     * Build fully resolved FIR node for a requested element.
     * This operation could be time-consuming because it creates FileStructureElement
     * and resolves non-local declaration into BODY phase.
     * Please use [getOrBuildFirFile] to get [FirFile] in undefined phase
     * @see getOrBuildFirFile
     */
    internal abstract fun getOrBuildFirFor(element: KtElement): FirElement?

    /**
     * Get or build or get cached [FirFile] for requested file in undefined phase
     */
    internal abstract fun getOrBuildFirFile(ktFile: KtFile): FirFile

    internal abstract fun getDiagnostics(element: KtElement, filter: DiagnosticCheckerFilter): List<KtPsiDiagnostic>

    internal abstract fun collectDiagnosticsForFile(ktFile: KtFile, filter: DiagnosticCheckerFilter): Collection<KtPsiDiagnostic>

    abstract fun resolveToFirSymbol(ktDeclaration: KtDeclaration, phase: FirResolvePhase): FirBasedSymbol<*>

    internal abstract fun resolveFirToPhase(declaration: FirDeclaration, toPhase: FirResolvePhase)

    abstract fun getTowerContextProvider(ktFile: KtFile): FirTowerContextProvider
}

fun LLFirResolveSession.getModule(element: PsiElement): KtModule {
    return ProjectStructureProvider.getModule(project, element, useSiteKtModule)
}