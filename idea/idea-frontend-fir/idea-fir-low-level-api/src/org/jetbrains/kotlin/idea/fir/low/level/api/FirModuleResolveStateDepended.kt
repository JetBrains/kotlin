/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.analyzer.ModuleInfo
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirPsiDiagnostic
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.idea.fir.low.level.api.annotations.InternalForInline
import org.jetbrains.kotlin.idea.fir.low.level.api.api.DiagnosticCheckerFilter
import org.jetbrains.kotlin.idea.fir.low.level.api.api.FirModuleResolveState
import org.jetbrains.kotlin.idea.fir.low.level.api.element.builder.FirTowerContextProvider
import org.jetbrains.kotlin.idea.fir.low.level.api.file.builder.ModuleFileCache
import org.jetbrains.kotlin.idea.fir.low.level.api.file.structure.KtToFirMapping
import org.jetbrains.kotlin.idea.fir.low.level.api.lazy.resolve.ResolveType
import org.jetbrains.kotlin.idea.fir.low.level.api.util.containingKtFileIfAny
import org.jetbrains.kotlin.idea.fir.low.level.api.util.originalKtFile
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtLambdaExpression

internal class FirModuleResolveStateDepended(
    private val originalState: FirModuleResolveStateImpl,
    val towerProviderBuiltUponElement: FirTowerContextProvider,
    private val ktToFirMapping: KtToFirMapping?,
) : FirModuleResolveState() {

    override val project: Project get() = originalState.project
    override val moduleInfo: ModuleInfo get() = originalState.moduleInfo
    override val rootModuleSession get() = originalState.rootModuleSession
    private val fileStructureCache get() = originalState.fileStructureCache

    override fun getSessionFor(moduleInfo: ModuleInfo): FirSession =
        originalState.getSessionFor(moduleInfo)

    override fun getOrBuildFirFor(element: KtElement): FirElement {
        val psi = originalState.elementBuilder.getPsiAsFirElementSource(element)

        ktToFirMapping?.getFirOfClosestParent(psi, this)?.let { return it }

        return originalState.elementBuilder.getOrBuildFirFor(
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

    override fun getDiagnostics(element: KtElement, filter: DiagnosticCheckerFilter): List<FirPsiDiagnostic<*>> =
        TODO("Diagnostics are not implemented for depended state")

    override fun collectDiagnosticsForFile(ktFile: KtFile, filter: DiagnosticCheckerFilter): Collection<FirPsiDiagnostic<*>> =
        TODO("Diagnostics are not implemented for depended state")

    @OptIn(InternalForInline::class)
    override fun findSourceFirDeclaration(ktDeclaration: KtLambdaExpression): FirDeclaration =
        originalState.findSourceFirDeclaration(ktDeclaration)

    @OptIn(InternalForInline::class)
    override fun findSourceFirDeclaration(ktDeclaration: KtDeclaration): FirDeclaration =
        originalState.findSourceFirDeclaration(ktDeclaration)

    @OptIn(InternalForInline::class)
    override fun findSourceFirCompiledDeclaration(ktDeclaration: KtDeclaration) =
        originalState.findSourceFirDeclaration(ktDeclaration)
}