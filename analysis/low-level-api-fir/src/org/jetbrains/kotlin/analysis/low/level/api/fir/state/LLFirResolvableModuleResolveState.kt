/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.state

import org.jetbrains.kotlin.analysis.low.level.api.fir.api.LLFirModuleResolveState
import org.jetbrains.kotlin.analysis.low.level.api.fir.element.builder.FirElementBuilder
import org.jetbrains.kotlin.analysis.low.level.api.fir.element.builder.FirTowerContextProvider
import org.jetbrains.kotlin.analysis.low.level.api.fir.element.builder.getNonLocalContainingOrThisDeclaration
import org.jetbrains.kotlin.analysis.low.level.api.fir.file.builder.FirFileBuilder
import org.jetbrains.kotlin.analysis.low.level.api.fir.file.builder.ModuleFileCache
import org.jetbrains.kotlin.analysis.low.level.api.fir.file.structure.FileStructureCache
import org.jetbrains.kotlin.analysis.low.level.api.fir.lazy.resolve.FirLazyDeclarationResolver
import org.jetbrains.kotlin.analysis.low.level.api.fir.providers.LLFirProvider
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.LLFirSessionProvider
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.LLFirSourcesSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.FirDeclarationForCompiledElementSearcher
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.findSourceNonLocalFirDeclaration
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.getElementTextInContext
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.originalDeclaration
import org.jetbrains.kotlin.analysis.project.structure.KtModule
import org.jetbrains.kotlin.analysis.project.structure.getKtModule
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.expressions.FirAnonymousFunctionExpression
import org.jetbrains.kotlin.fir.expressions.FirAnonymousObjectExpression
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.providers.firProvider
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.psi.*

internal abstract class LLFirResolvableModuleResolveState(
    protected val sessionProvider: LLFirSessionProvider,
    val firFileBuilder: FirFileBuilder,
    val firLazyDeclarationResolver: FirLazyDeclarationResolver,
) : LLFirModuleResolveState() {
    final override val rootModuleSession = sessionProvider.rootModuleSession
    val cache = (rootModuleSession.firProvider as LLFirProvider).cache

    val fileStructureCache = FileStructureCache(firFileBuilder, firLazyDeclarationResolver)
    val elementBuilder = FirElementBuilder()

    override fun getSessionFor(module: KtModule): FirSession =
        sessionProvider.getSession(module)!!

    override fun getOrBuildFirFor(element: KtElement): FirElement? =
        elementBuilder.getOrBuildFirFor(
            element = element,
            firFileBuilder = firFileBuilder,
            moduleFileCache = cache,
            fileStructureCache = fileStructureCache,
            firLazyDeclarationResolver = firLazyDeclarationResolver,
            state = this
        )


    override fun getOrBuildFirFile(ktFile: KtFile): FirFile =
        firFileBuilder.buildRawFirFileWithCaching(ktFile, cache)

    override fun tryGetCachedFirFile(declaration: FirDeclaration, cache: ModuleFileCache): FirFile? =
        cache.getContainerFirFile(declaration)

    override fun resolveToFirSymbol(
        ktDeclaration: KtDeclaration,
        phase: FirResolvePhase
    ): FirBasedSymbol<*> {
        val module = ktDeclaration.getKtModule()
        return when (getModuleKind(module)) {
            ModuleKind.RESOLVABLE_MODULE -> findSourceFirSymbol(ktDeclaration, module).also { resolveFirToPhase(it.fir, phase) }
            ModuleKind.BINARY_MODULE -> findFirCompiledSymbol(ktDeclaration)
        }
    }

    private fun findFirCompiledSymbol(ktDeclaration: KtDeclaration): FirBasedSymbol<*> {
        require(ktDeclaration.containingKtFile.isCompiled) {
            "This method will only work on compiled declarations, but this declaration is not compiled: ${ktDeclaration.getElementTextInContext()}"
        }

        val searcher = FirDeclarationForCompiledElementSearcher(rootModuleSession.symbolProvider)
        val firDeclaration = searcher.findNonLocalDeclaration(ktDeclaration)
        return firDeclaration.symbol
    }

    private fun findSourceFirSymbol(ktDeclaration: KtDeclaration, module: KtModule): FirBasedSymbol<*> {
        return findSourceFirDeclarationByExpression(ktDeclaration.originalDeclaration ?: ktDeclaration, module)
    }

    /**
     * [ktDeclaration] should be either [KtDeclaration] or [KtLambdaExpression]
     */
    private fun findSourceFirDeclarationByExpression(ktDeclaration: KtExpression, module: KtModule): FirBasedSymbol<*> {
        require(getModuleKind(module) == ModuleKind.RESOLVABLE_MODULE) {
            "Declaration should be resolvable module, instead it had ${module::class}"
        }
        val nonLocalNamedDeclaration = ktDeclaration.getNonLocalContainingOrThisDeclaration()
            ?: error("Declaration should have non-local container${ktDeclaration.getElementTextInContext()}")

        if (ktDeclaration == nonLocalNamedDeclaration) {
            return nonLocalNamedDeclaration.findSourceNonLocalFirDeclaration(
                firFileBuilder = firFileBuilder,
                firSymbolProvider = rootModuleSession.firProvider.symbolProvider,
                moduleFileCache = sessionProvider.getModuleCache(module)
            ).symbol
        }

        return findDeclarationInSourceViaResolve(ktDeclaration)
    }

    protected abstract fun getModuleKind(module: KtModule): ModuleKind

    private fun findDeclarationInSourceViaResolve(ktDeclaration: KtExpression): FirBasedSymbol<*> {
        val firDeclaration = when (val fir = getOrBuildFirFor(ktDeclaration)) {
            is FirDeclaration -> fir
            is FirAnonymousFunctionExpression -> fir.anonymousFunction
            is FirAnonymousObjectExpression -> fir.anonymousObject
            else -> error("FirDeclaration was not found for\n${ktDeclaration.getElementTextInContext()}")
        }
        return firDeclaration.symbol
    }

    override fun resolveFirToPhase(declaration: FirDeclaration, toPhase: FirResolvePhase) {
        if (toPhase == FirResolvePhase.RAW_FIR) return
        val fileCache = when (val session = declaration.moduleData.session) {
            is LLFirSourcesSession -> session.cache
            else -> return
        }
        firLazyDeclarationResolver.lazyResolveDeclaration(
            firDeclarationToResolve = declaration,
            moduleFileCache = fileCache,
            scopeSession = ScopeSession(),
            toPhase = toPhase,
            checkPCE = true,
        )
    }

    override fun getTowerContextProvider(ktFile: KtFile): FirTowerContextProvider {
        return TowerProviderForElementForState(this)
    }

    protected enum class ModuleKind {
        RESOLVABLE_MODULE,
        BINARY_MODULE
    }
}