/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.DiagnosticCheckerFilter
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.LLFirModuleResolveState
import org.jetbrains.kotlin.analysis.low.level.api.fir.element.builder.FirElementBuilder
import org.jetbrains.kotlin.analysis.low.level.api.fir.element.builder.FirTowerContextProvider
import org.jetbrains.kotlin.analysis.low.level.api.fir.file.builder.ModuleFileCache
import org.jetbrains.kotlin.analysis.low.level.api.fir.file.structure.KtToFirMapping
import org.jetbrains.kotlin.analysis.low.level.api.fir.state.LLFirResolvableModuleResolveState
import org.jetbrains.kotlin.analysis.low.level.api.fir.state.TowerProviderForElementForState
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.containingKtFileIfAny
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.originalKtFile
import org.jetbrains.kotlin.analysis.project.structure.KtModule
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

internal class FirModuleResolveStateDepended(
    val originalState: LLFirResolvableModuleResolveState,
    val towerProviderBuiltUponElement: FirTowerContextProvider,
    private val ktToFirMapping: KtToFirMapping?,
) : LLFirModuleResolveState() {
    override val project: Project get() = originalState.project
    override val module: KtModule get() = originalState.module
    override val rootModuleSession get() = originalState.rootModuleSession

    override fun getSessionFor(module: KtModule): FirSession =
        originalState.getSessionFor(module)

    override fun getOrBuildFirFor(element: KtElement): FirElement? {
        val psi = FirElementBuilder.getPsiAsFirElementSource(element) ?: return null
        ktToFirMapping?.getFirOfClosestParent(psi, this)?.let { return it }
        return originalState.getOrBuildFirFor(element = element)
    }

    override fun getOrBuildFirFile(ktFile: KtFile): FirFile =
        originalState.getOrBuildFirFile(ktFile)

    override fun resolveFirToPhase(declaration: FirDeclaration, toPhase: FirResolvePhase) {
        originalState.resolveFirToPhase(declaration, toPhase)
    }

    override fun tryGetCachedFirFile(declaration: FirDeclaration, cache: ModuleFileCache): FirFile? {
        val ktFile = declaration.containingKtFileIfAny ?: return null
        cache.getCachedFirFile(ktFile)?.let { return it }
        ktFile.originalKtFile?.let(cache::getCachedFirFile)?.let { return it }
        return null
    }

    override fun getDiagnostics(element: KtElement, filter: DiagnosticCheckerFilter): List<KtPsiDiagnostic> =
        TODO("Diagnostics are not implemented for depended state")

    override fun collectDiagnosticsForFile(ktFile: KtFile, filter: DiagnosticCheckerFilter): Collection<KtPsiDiagnostic> =
        TODO("Diagnostics are not implemented for depended state")

    override fun resolveToFirSymbol(ktDeclaration: KtDeclaration, phase: FirResolvePhase): FirBasedSymbol<*> {
        return originalState.resolveToFirSymbol(ktDeclaration, phase)
    }

    override fun getTowerContextProvider(ktFile: KtFile): FirTowerContextProvider =
        TowerProviderForElementForState(this)
}
