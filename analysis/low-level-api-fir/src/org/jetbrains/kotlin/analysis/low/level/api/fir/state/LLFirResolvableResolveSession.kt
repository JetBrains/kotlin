/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.state

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.ModificationTracker
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import org.jetbrains.kotlin.analysis.low.level.api.fir.LLFirModuleResolveComponents
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.LLFirResolveSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.getModule
import org.jetbrains.kotlin.analysis.low.level.api.fir.element.builder.FirTowerContextProvider
import org.jetbrains.kotlin.analysis.low.level.api.fir.element.builder.getNonLocalContainingDeclaration
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.LLFirResolvableModuleSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.LLFirSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.LLFirSessionCache
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.*
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.FirDeclarationForCompiledElementSearcher
import org.jetbrains.kotlin.analysis.project.structure.KtModule
import org.jetbrains.kotlin.utils.exceptions.errorWithAttachment
import org.jetbrains.kotlin.analysis.utils.errors.requireIsInstance
import org.jetbrains.kotlin.analysis.utils.errors.withPsiEntry
import org.jetbrains.kotlin.psi.psiUtil.getElementTextWithContext
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
    final override val useSiteKtModule: KtModule,
    private val useSiteSessionFactory: (KtModule) -> LLFirSession
) : LLFirResolveSession() {
    final override val project: Project
        get() = useSiteKtModule.project

    private val useSiteFirSessionCached = CachedValuesManager.getManager(project).createCachedValue {
        val session = useSiteSessionFactory(useSiteKtModule)
        CachedValueProvider.Result.create(session, session.createValidityTracker())
    }

    final override val useSiteFirSession: LLFirSession
        get() = useSiteFirSessionCached.value

    override fun getSessionFor(module: KtModule): LLFirSession {
        return getSession(module, preferBinary = true)
    }

    private fun getResolvableSessionFor(module: KtModule): LLFirResolvableModuleSession {
        return getSession(module, preferBinary = false) as LLFirResolvableModuleSession
    }

    private fun getSession(module: KtModule, preferBinary: Boolean): LLFirSession {
        if (module == useSiteFirSession.ktModule) {
            return useSiteFirSession
        }

        val cache = LLFirSessionCache.getInstance(module.project)
        return cache.getSession(module, preferBinary)
    }

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
        val module = getModule(element)
        return getResolvableSessionFor(module).moduleComponents
    }

    override fun resolveToFirSymbol(
        ktDeclaration: KtDeclaration,
        phase: FirResolvePhase,
    ): FirBasedSymbol<*> {
        val containingKtFile = ktDeclaration.containingKtFile
        val module = getModule(containingKtFile.originalKtFile ?: containingKtFile)
        return when (getModuleKind(module)) {
            ModuleKind.RESOLVABLE_MODULE -> findSourceFirSymbol(ktDeclaration, module).also { resolveFirToPhase(it.fir, phase) }
            ModuleKind.BINARY_MODULE -> findFirCompiledSymbol(ktDeclaration, module)
        }
    }

    private fun findFirCompiledSymbol(ktDeclaration: KtDeclaration, module: KtModule): FirBasedSymbol<*> {
        require(ktDeclaration.containingKtFile.isCompiled) {
            "This method will only work on compiled declarations, but this declaration is not compiled: ${ktDeclaration.getElementTextWithContext()}"
        }

        val session = getSessionFor(module)
        val searcher = FirDeclarationForCompiledElementSearcher(session.symbolProvider)
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

        val nonLocalDeclaration = getNonLocalContainingDeclaration(ktDeclaration.parentsWithSelfCodeFragmentAware)
            ?: errorWithAttachment("Declaration should have non-local container") {
                withPsiEntry("ktDeclaration", ktDeclaration, ::getModule)
                withEntry("module", module) { it.moduleDescription }
            }

        if (ktDeclaration == nonLocalDeclaration) {
            val session = getResolvableSessionFor(module)
            return nonLocalDeclaration.findSourceNonLocalFirDeclaration(
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