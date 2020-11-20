/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.psi
import org.jetbrains.kotlin.fir.resolve.providers.FirProvider
import org.jetbrains.kotlin.idea.caches.project.IdeaModuleInfo
import org.jetbrains.kotlin.idea.caches.project.ModuleSourceInfo
import org.jetbrains.kotlin.idea.caches.project.getModuleInfo
import org.jetbrains.kotlin.idea.fir.low.level.api.api.FirModuleResolveState
import org.jetbrains.kotlin.idea.fir.low.level.api.diagnostics.DiagnosticsCollector
import org.jetbrains.kotlin.idea.fir.low.level.api.element.builder.FirElementBuilder
import org.jetbrains.kotlin.idea.fir.low.level.api.element.builder.FirTowerDataContextCollector
import org.jetbrains.kotlin.idea.fir.low.level.api.element.builder.getNonLocalContainingOrThisDeclaration
import org.jetbrains.kotlin.idea.fir.low.level.api.file.builder.FirFileBuilder
import org.jetbrains.kotlin.idea.fir.low.level.api.file.builder.ModuleFileCache
import org.jetbrains.kotlin.idea.fir.low.level.api.file.structure.FileStructureCache
import org.jetbrains.kotlin.idea.fir.low.level.api.lazy.resolve.FirLazyDeclarationResolver
import org.jetbrains.kotlin.idea.fir.low.level.api.providers.firIdeProvider
import org.jetbrains.kotlin.idea.fir.low.level.api.sessions.FirIdeSessionProvider
import org.jetbrains.kotlin.idea.fir.low.level.api.sessions.FirIdeSourcesSession
import org.jetbrains.kotlin.idea.fir.low.level.api.util.FirElementFinder
import org.jetbrains.kotlin.idea.fir.low.level.api.util.findSourceNonLocalFirDeclaration
import org.jetbrains.kotlin.idea.util.getElementTextInContext
import org.jetbrains.kotlin.psi.*

internal class FirModuleResolveStateImpl(
    override val project: Project,
    override val moduleInfo: IdeaModuleInfo,
    private val sessionProvider: FirIdeSessionProvider,
    val firFileBuilder: FirFileBuilder,
    val firLazyDeclarationResolver: FirLazyDeclarationResolver,
) : FirModuleResolveState() {
    override val rootModuleSession: FirIdeSourcesSession get() = sessionProvider.rootModuleSession
    override val firTransformerProvider: FirTransformerProvider get() = firFileBuilder.firPhaseRunner.transformerProvider
    val fileStructureCache = FileStructureCache(firFileBuilder, firLazyDeclarationResolver)
    val elementBuilder = FirElementBuilder()
    private val diagnosticsCollector = DiagnosticsCollector(fileStructureCache, rootModuleSession.cache)

    override fun getSessionFor(moduleInfo: IdeaModuleInfo): FirSession =
        sessionProvider.getSession(moduleInfo)!!

    override fun getOrBuildFirFor(element: KtElement): FirElement =
        elementBuilder.getOrBuildFirFor(element, rootModuleSession.cache, fileStructureCache)

    override fun getFirFile(ktFile: KtFile): FirFile =
        firFileBuilder.buildRawFirFileWithCaching(ktFile, rootModuleSession.cache, lazyBodiesMode = false)

    override fun getDiagnostics(element: KtElement): List<Diagnostic> =
        diagnosticsCollector.getDiagnosticsFor(element)

    override fun collectDiagnosticsForFile(ktFile: KtFile): Collection<Diagnostic> =
        diagnosticsCollector.collectDiagnosticsForFile(ktFile)

    override fun getBuiltFirFileOrNull(ktFile: KtFile): FirFile? {
        val cache = sessionProvider.getModuleCache(ktFile.getModuleInfo() as ModuleSourceInfo)
        return firFileBuilder.getBuiltFirFileOrNull(ktFile, cache)
    }

    override fun recordPsiToFirMappingsForCompletionFrom(fir: FirDeclaration, firFile: FirFile, ktFile: KtFile) {
        error("Should be called only from FirModuleResolveStateForCompletion")
    }

    override fun findNonLocalSourceFirDeclaration(
        ktDeclaration: KtDeclaration,
    ): FirDeclaration = ktDeclaration.findSourceNonLocalFirDeclaration(
        firFileBuilder,
        rootModuleSession.firIdeProvider.symbolProvider,
        sessionProvider.getModuleCache(ktDeclaration.getModuleInfo() as ModuleSourceInfo)
    )

    override fun findSourceFirDeclaration(ktDeclaration: KtDeclaration): FirDeclaration =
        findSourceFirDeclarationByExpression(ktDeclaration)

    override fun findSourceFirDeclaration(ktDeclaration: KtLambdaExpression): FirDeclaration =
        findSourceFirDeclarationByExpression(ktDeclaration)

    /**
     * [ktDeclaration] should be either [KtDeclaration] or [KtLambdaExpression]
     */
    private fun findSourceFirDeclarationByExpression(ktDeclaration: KtExpression): FirDeclaration {
        val nonLocalFirDeclaration = ktDeclaration.getNonLocalContainingOrThisDeclaration()
            ?: error("Declaration should have non-local container${ktDeclaration.getElementTextInContext()}")
        if (ktDeclaration == nonLocalFirDeclaration) return findNonLocalSourceFirDeclaration(ktDeclaration as KtDeclaration)
        val container = nonLocalFirDeclaration.findSourceNonLocalFirDeclaration(
            firFileBuilder,
            rootModuleSession.firIdeProvider.symbolProvider,
            sessionProvider.getModuleCache(ktDeclaration.getModuleInfo() as ModuleSourceInfo)
        )
        val firDeclaration = FirElementFinder.findElementIn<FirDeclaration>(container) { firDeclaration ->
            firDeclaration.psi == ktDeclaration
        }
        return firDeclaration
            ?: error("FirDeclaration was not found for\n${ktDeclaration.getElementTextInContext()}")
    }

    override fun isFirFileBuilt(ktFile: KtFile): Boolean {
        val moduleSourceInfo = ktFile.getModuleInfo() as? ModuleSourceInfo ?: return true
        val cache = sessionProvider.getModuleCache(moduleSourceInfo)
        return firFileBuilder.isFirFileBuilt(ktFile, cache)
    }

    override fun <D : FirDeclaration> resolvedFirToPhase(declaration: D, toPhase: FirResolvePhase): D {
        val fileCache = when (val session = declaration.session) {
            is FirIdeSourcesSession -> session.cache
            else -> return declaration
        }
        firLazyDeclarationResolver.lazyResolveDeclaration(declaration, fileCache, toPhase, checkPCE = true)
        return declaration
    }

    override fun lazyResolveDeclarationForCompletion(
        firFunction: FirDeclaration,
        containerFirFile: FirFile,
        firIdeProvider: FirProvider,
        toPhase: FirResolvePhase,
        towerDataContextCollector: FirTowerDataContextCollector
    ) {
        firFileBuilder.runCustomResolveWithPCECheck(containerFirFile, rootModuleSession.cache) {
            firLazyDeclarationResolver.runLazyResolveWithoutLock(
                firFunction,
                rootModuleSession.cache,
                containerFirFile,
                firIdeProvider,
                fromPhase = firFunction.resolvePhase,
                toPhase,
                towerDataContextCollector,
                checkPCE = true
            )
        }
    }

    override fun getFirFile(declaration: FirDeclaration, cache: ModuleFileCache): FirFile? =
        cache.getContainerFirFile(declaration)
}