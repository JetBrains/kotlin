/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.state

import org.jetbrains.kotlin.analysis.low.level.api.fir.LLFirGlobalResolveComponents
import org.jetbrains.kotlin.analysis.low.level.api.fir.LLFirModuleResolveComponents
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.LLFirResolveSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.element.builder.FirTowerContextProvider
import org.jetbrains.kotlin.analysis.low.level.api.fir.element.builder.getNonLocalContainingOrThisDeclaration
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.LLFirSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.LLFirSessionProvider
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.FirDeclarationForCompiledElementSearcher
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.errorWithFirSpecificEntries
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.findSourceNonLocalFirDeclaration
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.originalDeclaration
import org.jetbrains.kotlin.analysis.project.structure.KtModule
import org.jetbrains.kotlin.analysis.project.structure.getKtModule
import org.jetbrains.kotlin.analysis.utils.errors.buildErrorWithAttachment
import org.jetbrains.kotlin.analysis.utils.errors.requireIsInstance
import org.jetbrains.kotlin.analysis.utils.errors.withPsiEntry
import org.jetbrains.kotlin.analysis.utils.printer.getElementTextInContext
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
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhase
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtFile

internal abstract class LLFirResolvableResolveSession(
    private val sessionProvider: LLFirSessionProvider,
) : LLFirResolveSession() {
    abstract val globalComponents: LLFirGlobalResolveComponents

    final override val useSiteFirSession = sessionProvider.rootModuleSession

    override fun getSessionFor(module: KtModule): LLFirSession =
        sessionProvider.getSession(module)

    override fun getScopeSessionFor(firSession: FirSession): ScopeSession {
        requireIsInstance<LLFirSession>(firSession)
        return firSession.getScopeSession()
    }

    override fun getOrBuildFirFor(element: KtElement): FirElement? {
        val moduleComponents = getModuleComponentsForElement(element)
        return moduleComponents.elementsBuilder.getOrBuildFirFor(element, this)
    }

    override fun getOrBuildFirFile(ktFile: KtFile): FirFile {
        val moduleComponents = getModuleComponentsForElement(ktFile)
        return moduleComponents.firFileBuilder.buildRawFirFileWithCaching(ktFile)
    }

    protected fun getModuleComponentsForElement(element: KtElement): LLFirModuleResolveComponents {
        val ktModule = element.getKtModule()
        return sessionProvider.getResolvableSession(ktModule).moduleComponents
    }

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

        val ktModule = ktDeclaration.getKtModule(project)
        val firSession = sessionProvider.getSession(ktModule)
        val searcher = FirDeclarationForCompiledElementSearcher(firSession.symbolProvider)
        val firDeclaration = searcher.findNonLocalDeclaration(ktDeclaration)
        return firDeclaration.symbol
    }

    private fun findSourceFirSymbol(ktDeclaration: KtDeclaration, module: KtModule): FirBasedSymbol<*> {
        return findSourceFirDeclarationByDeclaration(ktDeclaration.originalDeclaration ?: ktDeclaration, module)
    }

    private fun findSourceFirDeclarationByDeclaration(ktDeclaration: KtDeclaration, module: KtModule): FirBasedSymbol<*> {
        require(getModuleKind(module) == ModuleKind.RESOLVABLE_MODULE) {
            "Declaration should be resolvable module, instead it had ${module::class}"
        }
        val nonLocalNamedDeclaration = ktDeclaration.getNonLocalContainingOrThisDeclaration()
            ?: buildErrorWithAttachment("Declaration should have non-local container") {
                withPsiEntry("ktDeclaration", ktDeclaration)
                withEntry("module", module) { it.moduleDescription }
            }

        if (ktDeclaration == nonLocalNamedDeclaration) {
            val session = sessionProvider.getResolvableSession(module)
            return nonLocalNamedDeclaration.findSourceNonLocalFirDeclaration(
                firFileBuilder = session.moduleComponents.firFileBuilder,
                provider = session.firProvider,
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
            else -> errorWithFirSpecificEntries(
                "FirDeclaration was not found for ${ktDeclaration::class}, fir is ${fir?.let { it::class }}",
                fir = fir,
                psi = ktDeclaration,
            )
        }
        return firDeclaration.symbol
    }

    override fun resolveFirToPhase(declaration: FirDeclaration, toPhase: FirResolvePhase) {
        declaration.lazyResolveToPhase(toPhase)
    }

    override fun getTowerContextProvider(ktFile: KtFile): FirTowerContextProvider {
        return TowerProviderForElementForState(this)
    }

    protected enum class ModuleKind {
        RESOLVABLE_MODULE,
        BINARY_MODULE
    }
}