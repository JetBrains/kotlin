/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.api

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.analysis.low.level.api.fir.file.builder.ModuleFileCache
import org.jetbrains.kotlin.analysis.project.structure.KtLibraryModule
import org.jetbrains.kotlin.analysis.project.structure.KtModule
import org.jetbrains.kotlin.analysis.project.structure.KtSourceModule
import org.jetbrains.kotlin.analysis.project.structure.getKtModule
import org.jetbrains.kotlin.diagnostics.KtPsiDiagnostic
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtLambdaExpression

abstract class FirModuleResolveState {
    abstract val project: Project

    abstract val rootModuleSession: FirSession

    abstract val module: KtModule

    internal abstract fun getSessionFor(module: KtModule): FirSession

    /**
     * Build fully resolved FIR node for requested element.
     * This operation could be performance affective because it create FIleStructureElement and resolve non-local declaration into BODY phase, use
     * @see tryGetCachedFirFile to get [FirFile] in undefined phase
     */
    internal abstract fun getOrBuildFirFor(element: KtElement): FirElement?

    /**
     * Get or build or get cached [FirFile] for requested file in undefined phase
     */
    internal abstract fun getOrBuildFirFile(ktFile: KtFile): FirFile

    /**
     * Try get [FirFile] from the cache in undefined phase
     */
    internal abstract fun tryGetCachedFirFile(declaration: FirDeclaration, cache: ModuleFileCache): FirFile?

    internal abstract fun getDiagnostics(element: KtElement, filter: DiagnosticCheckerFilter): List<KtPsiDiagnostic>

    internal abstract fun collectDiagnosticsForFile(ktFile: KtFile, filter: DiagnosticCheckerFilter): Collection<KtPsiDiagnostic>

    abstract fun findSourceFirSymbol(
        ktDeclaration: KtDeclaration,
    ): FirBasedSymbol<*>

    abstract fun findSourceFirSymbol(
        ktDeclaration: KtLambdaExpression,
    ): FirBasedSymbol<*>

    /**
     * Looks for compiled non-local [ktDeclaration] declaration by querying its classId/callableId from the SymbolProvider.
     *
     * Works only if [ktDeclaration] is compiled (i.e. comes from .class file).
     */
    abstract fun findSourceFirCompiledSymbol(
        ktDeclaration: KtDeclaration
    ): FirBasedSymbol<*>


    open fun resolveToFirSymbol(
        ktDeclaration: KtDeclaration,
        resolveState: FirModuleResolveState,
        phase: FirResolvePhase = FirResolvePhase.RAW_FIR,
    ): FirBasedSymbol<*> {
        return when (val module = ktDeclaration.getKtModule()) {
            is KtSourceModule -> resolveState.findSourceFirSymbol(ktDeclaration).also { resolveState.resolveFirToPhase(it.fir, phase) }
            is KtLibraryModule -> resolveState.findSourceFirCompiledSymbol(ktDeclaration)
            else -> error("unsupported module $module")
        }
    }

    internal abstract fun resolveFirToPhase(declaration: FirDeclaration, toPhase: FirResolvePhase)


}
