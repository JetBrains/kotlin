/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api

import com.intellij.openapi.project.Project
import org.jetbrains.annotations.TestOnly
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirPsiDiagnostic
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.realPsi
import org.jetbrains.kotlin.idea.caches.project.IdeaModuleInfo
import org.jetbrains.kotlin.idea.caches.project.ModuleSourceInfo
import org.jetbrains.kotlin.idea.caches.project.getModuleInfo
import org.jetbrains.kotlin.idea.fir.low.level.api.annotations.InternalForInline
import org.jetbrains.kotlin.idea.fir.low.level.api.api.DiagnosticCheckerFilter
import org.jetbrains.kotlin.idea.fir.low.level.api.api.FirModuleResolveState
import org.jetbrains.kotlin.idea.fir.low.level.api.diagnostics.DiagnosticsCollector
import org.jetbrains.kotlin.idea.fir.low.level.api.element.builder.FirElementBuilder
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

    /**
     * WARNING! This object contains scopes for all statements and declarations that were ever resolved.
     * It can grow unbounded if you never edit the files in the opened project.
     *
     * It is a temporary solution until we can retrieve scopes for any fir element without re-resolving it.
     */
    val fileStructureCache = FileStructureCache(firFileBuilder, firLazyDeclarationResolver)
    val elementBuilder = FirElementBuilder()
    private val diagnosticsCollector = DiagnosticsCollector(fileStructureCache, rootModuleSession.cache)

    override fun getSessionFor(moduleInfo: IdeaModuleInfo): FirSession =
        sessionProvider.getSession(moduleInfo)!!

    override fun getOrBuildFirFor(element: KtElement): FirElement =
        elementBuilder.getOrBuildFirFor(element, firFileBuilder, rootModuleSession.cache, fileStructureCache)

    override fun getFirFile(ktFile: KtFile): FirFile =
        firFileBuilder.buildRawFirFileWithCaching(ktFile, rootModuleSession.cache, lazyBodiesMode = false)

    override fun getDiagnostics(element: KtElement, filter: DiagnosticCheckerFilter): List<FirPsiDiagnostic<*>> =
        diagnosticsCollector.getDiagnosticsFor(element, filter)

    override fun collectDiagnosticsForFile(ktFile: KtFile, filter: DiagnosticCheckerFilter): Collection<FirPsiDiagnostic<*>> =
        diagnosticsCollector.collectDiagnosticsForFile(ktFile, filter)

    @TestOnly
    internal fun getBuiltFirFileOrNull(ktFile: KtFile): FirFile? {
        val cache = sessionProvider.getModuleCache(ktFile.getModuleInfo() as ModuleSourceInfo)
        return firFileBuilder.getBuiltFirFileOrNull(ktFile, cache)
    }

    @OptIn(InternalForInline::class)
    override fun findSourceFirDeclaration(ktDeclaration: KtDeclaration): FirDeclaration =
        findSourceFirDeclarationByExpression(ktDeclaration)

    @OptIn(InternalForInline::class)
    override fun findSourceFirDeclaration(ktDeclaration: KtLambdaExpression): FirDeclaration =
        findSourceFirDeclarationByExpression(ktDeclaration)

    /**
     * [ktDeclaration] should be either [KtDeclaration] or [KtLambdaExpression]
     */
    private fun findSourceFirDeclarationByExpression(ktDeclaration: KtExpression): FirDeclaration {
        val nonLocalNamedDeclaration = ktDeclaration.getNonLocalContainingOrThisDeclaration()
            ?: error("Declaration should have non-local container${ktDeclaration.getElementTextInContext()}")

        val nonLocalFirForNamedDeclaration = nonLocalNamedDeclaration.findSourceNonLocalFirDeclaration(
            firFileBuilder,
            rootModuleSession.firIdeProvider.symbolProvider,
            sessionProvider.getModuleCache(ktDeclaration.getModuleInfo() as ModuleSourceInfo)
        )

        if (ktDeclaration == nonLocalNamedDeclaration) return nonLocalFirForNamedDeclaration

        if (nonLocalFirForNamedDeclaration.resolvePhase < FirResolvePhase.BODY_RESOLVE) {
            val cache = (nonLocalFirForNamedDeclaration.moduleData.session as FirIdeSourcesSession).cache
            firLazyDeclarationResolver.lazyResolveDeclaration(
                nonLocalFirForNamedDeclaration,
                cache,
                FirResolvePhase.BODY_RESOLVE,
                checkPCE = false, /*TODO*/
            )
        }
        val firDeclaration = FirElementFinder.findElementIn<FirDeclaration>(nonLocalFirForNamedDeclaration) { firDeclaration ->
            when (val realPsi = firDeclaration.realPsi) {
                is KtObjectLiteralExpression -> realPsi.objectDeclaration == ktDeclaration
                is KtFunctionLiteral -> realPsi.parent == ktDeclaration
                else -> realPsi == ktDeclaration
            }
        }
        return firDeclaration
            ?: error("FirDeclaration was not found for\n${ktDeclaration.getElementTextInContext()}")
    }

    override fun <D : FirDeclaration> resolvedFirToPhase(declaration: D, toPhase: FirResolvePhase): D {
        val fileCache = when (val session = declaration.moduleData.session) {
            is FirIdeSourcesSession -> session.cache
            else -> return declaration
        }
        firLazyDeclarationResolver.lazyResolveDeclaration(
            declaration,
            fileCache,
            toPhase,
            checkPCE = true,
            towerDataContextCollector = null,
        )
        return declaration
    }

    override fun getFirFile(declaration: FirDeclaration, cache: ModuleFileCache): FirFile? =
        cache.getContainerFirFile(declaration)
}