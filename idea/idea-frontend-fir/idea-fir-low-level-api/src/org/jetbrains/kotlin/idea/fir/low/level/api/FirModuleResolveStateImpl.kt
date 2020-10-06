/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api

import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.resolve.providers.FirProvider
import org.jetbrains.kotlin.idea.caches.project.IdeaModuleInfo
import org.jetbrains.kotlin.idea.caches.project.ModuleSourceInfo
import org.jetbrains.kotlin.idea.caches.project.getModuleInfo
import org.jetbrains.kotlin.idea.fir.low.level.api.api.FirModuleResolveState
import org.jetbrains.kotlin.idea.fir.low.level.api.diagnostics.DiagnosticsCollector
import org.jetbrains.kotlin.idea.fir.low.level.api.element.builder.FirElementBuilder
import org.jetbrains.kotlin.idea.fir.low.level.api.element.builder.FirTowerDataContextCollector
import org.jetbrains.kotlin.idea.fir.low.level.api.file.builder.FirFileBuilder
import org.jetbrains.kotlin.idea.fir.low.level.api.file.structure.FileStructureCache
import org.jetbrains.kotlin.idea.fir.low.level.api.lazy.resolve.FirLazyDeclarationResolver
import org.jetbrains.kotlin.idea.fir.low.level.api.providers.firIdeProvider
import org.jetbrains.kotlin.idea.fir.low.level.api.sessions.FirIdeSessionProvider
import org.jetbrains.kotlin.idea.fir.low.level.api.sessions.FirIdeSourcesSession
import org.jetbrains.kotlin.idea.fir.low.level.api.util.findSourceNonLocalFirDeclaration
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile

internal class FirModuleResolveStateImpl(
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
        firFileBuilder.buildRawFirFileWithCaching(ktFile, rootModuleSession.cache)

    override fun getDiagnostics(element: KtElement): List<Diagnostic> =
        diagnosticsCollector.getDiagnosticsFor(element)

    override fun collectDiagnosticsForFile(ktFile: KtFile): Collection<Diagnostic> =
        diagnosticsCollector.collectDiagnosticsForFile(ktFile)

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
}