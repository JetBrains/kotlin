/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.analysis.api.impl.barebone.annotations.InternalForInline
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.DiagnosticCheckerFilter
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.FirModuleResolveState
import org.jetbrains.kotlin.analysis.low.level.api.fir.element.builder.FirTowerContextProvider
import org.jetbrains.kotlin.analysis.low.level.api.fir.file.builder.ModuleFileCache
import org.jetbrains.kotlin.analysis.low.level.api.fir.file.structure.KtToFirMapping
import org.jetbrains.kotlin.analysis.low.level.api.fir.lazy.resolve.ResolveType
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.containingKtFileIfAny
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.originalKtFile
import org.jetbrains.kotlin.analysis.project.structure.KtModule
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirPsiDiagnostic
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtLambdaExpression

internal class FirModuleResolveStateDepended(
    val originalState: FirModuleResolveStateImpl,
    val towerProviderBuiltUponElement: FirTowerContextProvider,
    private val ktToFirMapping: KtToFirMapping?,
) : FirModuleResolveState() {

    override val project: Project get() = originalState.project
    override val module: KtModule get() = originalState.module
    override val rootModuleSession get() = originalState.rootModuleSession
    private val fileStructureCache get() = originalState.fileStructureCache

    override fun getSessionFor(module: KtModule): FirSession =
        originalState.getSessionFor(module)

    override fun getOrBuildFirFor(element: KtElement): FirElement? {
        val elementBuilder = originalState.elementBuilder

        val psi = elementBuilder.getPsiAsFirElementSource(element) ?: return null
        if (!elementBuilder.doKtElementHasCorrespondingFirElement(element)) return null

        ktToFirMapping?.getFirOfClosestParent(psi, this)?.let { return it }

        return elementBuilder.getOrBuildFirFor(
            element = element,
            firFileBuilder = originalState.firFileBuilder,
            moduleFileCache = originalState.rootModuleSession.cache,
            fileStructureCache = fileStructureCache,
            firLazyDeclarationResolver = originalState.firLazyDeclarationResolver,
            state = this,
        )
    }

    override fun getOrBuildFirFile(ktFile: KtFile): FirFile =
        originalState.getOrBuildFirFile(ktFile)

    override fun <D : FirDeclaration> resolveFirToPhase(declaration: D, toPhase: FirResolvePhase): D =
        originalState.resolveFirToPhase(declaration, toPhase)

    override fun <D : FirDeclaration> resolveFirToResolveType(declaration: D, type: ResolveType): D =
        originalState.resolveFirToResolveType(declaration, type)

    override fun tryGetCachedFirFile(declaration: FirDeclaration, cache: ModuleFileCache): FirFile? {
        val ktFile = declaration.containingKtFileIfAny ?: return null
        cache.getCachedFirFile(ktFile)?.let { return it }
        ktFile.originalKtFile?.let(cache::getCachedFirFile)?.let { return it }
        return null
    }

    override fun getDiagnostics(element: KtElement, filter: DiagnosticCheckerFilter): List<FirPsiDiagnostic> =
        TODO("Diagnostics are not implemented for depended state")

    override fun collectDiagnosticsForFile(ktFile: KtFile, filter: DiagnosticCheckerFilter): Collection<FirPsiDiagnostic> =
        TODO("Diagnostics are not implemented for depended state")

    @OptIn(InternalForInline::class)
    override fun findSourceFirDeclaration(ktDeclaration: KtLambdaExpression): FirDeclaration =
        originalState.findSourceFirDeclaration(ktDeclaration)

    @OptIn(InternalForInline::class)
    override fun findSourceFirDeclaration(ktDeclaration: KtDeclaration): FirDeclaration =
        originalState.findSourceFirDeclaration(ktDeclaration)

    @OptIn(InternalForInline::class)
    override fun findSourceFirCompiledDeclaration(ktDeclaration: KtDeclaration) =
        originalState.findSourceFirCompiledDeclaration(ktDeclaration)
}
