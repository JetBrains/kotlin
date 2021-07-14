/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.analyzer.ModuleInfo
import org.jetbrains.kotlin.analyzer.ModuleSourceInfoBase
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirPsiDiagnostic
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.realPsi
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.symbolProvider
import org.jetbrains.kotlin.idea.fir.low.level.api.annotations.InternalForInline
import org.jetbrains.kotlin.idea.fir.low.level.api.api.DiagnosticCheckerFilter
import org.jetbrains.kotlin.idea.fir.low.level.api.api.FirModuleResolveState
import org.jetbrains.kotlin.idea.fir.low.level.api.api.getModuleInfo
import org.jetbrains.kotlin.idea.fir.low.level.api.diagnostics.DiagnosticsCollector
import org.jetbrains.kotlin.idea.fir.low.level.api.element.builder.FirElementBuilder
import org.jetbrains.kotlin.idea.fir.low.level.api.element.builder.getNonLocalContainingOrThisDeclaration
import org.jetbrains.kotlin.idea.fir.low.level.api.file.builder.FirFileBuilder
import org.jetbrains.kotlin.idea.fir.low.level.api.file.builder.ModuleFileCache
import org.jetbrains.kotlin.idea.fir.low.level.api.file.structure.FileStructureCache
import org.jetbrains.kotlin.idea.fir.low.level.api.lazy.resolve.FirLazyDeclarationResolver
import org.jetbrains.kotlin.idea.fir.low.level.api.lazy.resolve.ResolveType
import org.jetbrains.kotlin.idea.fir.low.level.api.lazy.resolve.lazyResolveDeclaration
import org.jetbrains.kotlin.idea.fir.low.level.api.providers.firIdeProvider
import org.jetbrains.kotlin.idea.fir.low.level.api.sessions.FirIdeSessionProvider
import org.jetbrains.kotlin.idea.fir.low.level.api.sessions.FirIdeSourcesSession
import org.jetbrains.kotlin.idea.fir.low.level.api.util.FirDeclarationForCompiledElementSearcher
import org.jetbrains.kotlin.idea.fir.low.level.api.util.FirElementFinder
import org.jetbrains.kotlin.idea.fir.low.level.api.util.findSourceNonLocalFirDeclaration
import org.jetbrains.kotlin.idea.fir.low.level.api.util.getElementTextInContext
import org.jetbrains.kotlin.psi.*

internal class FirModuleResolveStateImpl(
    override val project: Project,
    override val moduleInfo: ModuleInfo,
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

    override fun getSessionFor(moduleInfo: ModuleInfo): FirSession =
        sessionProvider.getSession(moduleInfo)!!

    override fun getOrBuildFirFor(element: KtElement): FirElement =
        elementBuilder.getOrBuildFirFor(
            element = element,
            firFileBuilder = firFileBuilder,
            moduleFileCache = rootModuleSession.cache,
            fileStructureCache = fileStructureCache,
            firLazyDeclarationResolver = firLazyDeclarationResolver,
            state = this
        )

    override fun getOrBuildFirFile(ktFile: KtFile): FirFile =
        firFileBuilder.buildRawFirFileWithCaching(ktFile, rootModuleSession.cache, preferLazyBodies = false)

    override fun tryGetCachedFirFile(declaration: FirDeclaration, cache: ModuleFileCache): FirFile? =
        cache.getContainerFirFile(declaration)

    override fun getDiagnostics(element: KtElement, filter: DiagnosticCheckerFilter): List<FirPsiDiagnostic> =
        diagnosticsCollector.getDiagnosticsFor(element, filter)

    override fun collectDiagnosticsForFile(ktFile: KtFile, filter: DiagnosticCheckerFilter): Collection<FirPsiDiagnostic> =
        diagnosticsCollector.collectDiagnosticsForFile(ktFile, filter)

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
        require(ktDeclaration.getModuleInfo() is ModuleSourceInfoBase) {
            "Declaration should have ModuleSourceInfo, instead it had ${ktDeclaration.getModuleInfo()}"
        }
        val nonLocalNamedDeclaration = ktDeclaration.getNonLocalContainingOrThisDeclaration()
            ?: error("Declaration should have non-local container${ktDeclaration.getElementTextInContext()}")

        val nonLocalFirForNamedDeclaration = nonLocalNamedDeclaration.findSourceNonLocalFirDeclaration(
            firFileBuilder,
            rootModuleSession.firIdeProvider.symbolProvider,
            sessionProvider.getModuleCache(ktDeclaration.getModuleInfo() as ModuleSourceInfoBase)
        )

        if (ktDeclaration == nonLocalNamedDeclaration) return nonLocalFirForNamedDeclaration

        firLazyDeclarationResolver.lazyResolveDeclaration(
            firDeclarationToResolve = nonLocalFirForNamedDeclaration,
            scopeSession = ScopeSession(),
            moduleFileCache = (nonLocalFirForNamedDeclaration.moduleData.session as FirIdeSourcesSession).cache,
            toPhase = FirResolvePhase.BODY_RESOLVE,
            checkPCE = false, /*TODO*/
        )
        val firDeclaration = FirElementFinder.findElementIn<FirDeclaration>(nonLocalFirForNamedDeclaration) { firDeclaration ->
            when (val realPsi = firDeclaration.realPsi) {
                is KtObjectLiteralExpression -> realPsi.objectDeclaration == ktDeclaration
                is KtLambdaExpression -> realPsi.functionLiteral == ktDeclaration
                else -> realPsi == ktDeclaration
            }
        }
        return firDeclaration
            ?: error("FirDeclaration was not found for\n${ktDeclaration.getElementTextInContext()}")
    }

    @OptIn(InternalForInline::class)
    override fun findSourceFirCompiledDeclaration(ktDeclaration: KtDeclaration): FirDeclaration {
        require(ktDeclaration.containingKtFile.isCompiled) {
            "This method will only work on compiled declarations, but this declaration is not compiled: ${ktDeclaration.getElementTextInContext()}"
        }

        val searcher = FirDeclarationForCompiledElementSearcher(rootModuleSession.symbolProvider)

        return when (ktDeclaration) {
            is KtClassOrObject -> searcher.findNonLocalClass(ktDeclaration)
            is KtConstructor<*> -> searcher.findConstructorOfNonLocalClass(ktDeclaration)
            is KtNamedFunction -> searcher.findNonLocalFunction(ktDeclaration)
            is KtProperty -> searcher.findNonLocalProperty(ktDeclaration)

            else -> error("Unsupported compiled declaration of type ${ktDeclaration::class}: ${ktDeclaration.getElementTextInContext()}")
        }
    }

    override fun <D : FirDeclaration> resolveFirToPhase(declaration: D, toPhase: FirResolvePhase): D {
        if (toPhase == FirResolvePhase.RAW_FIR) return declaration
        val fileCache = when (val session = declaration.moduleData.session) {
            is FirIdeSourcesSession -> session.cache
            else -> return declaration
        }
        firLazyDeclarationResolver.lazyResolveDeclaration(
            firDeclarationToResolve = declaration,
            moduleFileCache = fileCache,
            scopeSession = ScopeSession(),
            toPhase = toPhase,
            checkPCE = true,
        )
        return declaration
    }

    override fun <D : FirDeclaration> resolveFirToResolveType(declaration: D, type: ResolveType): D {
        if (type == ResolveType.NoResolve) return declaration
        val fileCache = when (val session = declaration.moduleData.session) {
            is FirIdeSourcesSession -> session.cache
            else -> return declaration
        }
        firLazyDeclarationResolver.lazyResolveDeclaration(
            firDeclaration = declaration,
            moduleFileCache = fileCache,
            scopeSession = ScopeSession(),
            toResolveType = type,
            checkPCE = true,
        )
        return declaration
    }
}
