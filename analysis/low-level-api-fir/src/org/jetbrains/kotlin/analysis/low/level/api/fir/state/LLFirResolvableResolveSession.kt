/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.state

import org.jetbrains.kotlin.analysis.low.level.api.fir.LLFirModuleResolveComponents
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.LLFirResolveSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.getModule
import org.jetbrains.kotlin.analysis.low.level.api.fir.element.builder.FirTowerContextProvider
import org.jetbrains.kotlin.analysis.low.level.api.fir.element.builder.getNonLocalContainingDeclaration
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.*
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.FirDeclarationForCompiledElementSearcher
import org.jetbrains.kotlin.analysis.project.structure.KtModule
import org.jetbrains.kotlin.analysis.utils.errors.withPsiEntry
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.expressions.FirAnonymousFunctionExpression
import org.jetbrains.kotlin.fir.expressions.FirAnonymousObjectExpression
import org.jetbrains.kotlin.fir.resolve.providers.firProvider
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhase
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.psiUtil.getElementTextWithContext
import org.jetbrains.kotlin.utils.exceptions.errorWithAttachment

internal class LLFirResolvableResolveSession(
    moduleProvider: LLModuleProvider,
    resolutionStrategyProvider: LLModuleResolutionStrategyProvider,
    sessionProvider: LLSessionProvider,
    diagnosticProvider: LLDiagnosticProvider
) : LLFirResolveSession(
    moduleProvider = moduleProvider,
    resolutionStrategyProvider = resolutionStrategyProvider,
    sessionProvider = sessionProvider,
    scopeSessionProvider = LLDefaultScopeSessionProvider,
    diagnosticProvider = diagnosticProvider
) {
    override fun getOrBuildFirFor(element: KtElement): FirElement? {
        val moduleComponents = getModuleComponentsForElement(element)
        return moduleComponents.elementsBuilder.getOrBuildFirFor(element, this)
    }

    override fun getOrBuildFirFile(ktFile: KtFile): FirFile {
        val moduleComponents = getModuleComponentsForElement(ktFile)
        return moduleComponents.firFileBuilder.buildRawFirFileWithCaching(ktFile)
    }

    private fun getModuleComponentsForElement(element: KtElement): LLFirModuleResolveComponents {
        val module = getModule(element)
        return sessionProvider.getResolvableSession(module).moduleComponents
    }

    override fun resolveToFirSymbol(
        ktDeclaration: KtDeclaration,
        phase: FirResolvePhase,
    ): FirBasedSymbol<*> {
        val containingKtFile = ktDeclaration.containingKtFile
        val module = getModule(containingKtFile.originalKtFile ?: containingKtFile)
        return when (getModuleResolutionStrategy(module)) {
            LLModuleResolutionStrategy.LAZY -> findSourceFirSymbol(ktDeclaration, module).also { resolveFirToPhase(it.fir, phase) }
            LLModuleResolutionStrategy.STATIC -> findFirCompiledSymbol(ktDeclaration, module)
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
        require(getModuleResolutionStrategy(module) == LLModuleResolutionStrategy.LAZY) {
            "Declaration should be resolvable module, instead it had ${module::class}"
        }

        val nonLocalDeclaration = getNonLocalContainingDeclaration(ktDeclaration.parentsWithSelfCodeFragmentAware)
            ?: errorWithAttachment("Declaration should have non-local container") {
                withPsiEntry("ktDeclaration", ktDeclaration, ::getModule)
                withEntry("module", module) { it.moduleDescription }
            }

        if (ktDeclaration == nonLocalDeclaration) {
            val session = sessionProvider.getResolvableSession(module)
            return nonLocalDeclaration.findSourceNonLocalFirDeclaration(
                firFileBuilder = session.moduleComponents.firFileBuilder,
                provider = session.firProvider,
            ).symbol
        }

        return findDeclarationInSourceViaResolve(ktDeclaration)
    }

    private fun getModuleResolutionStrategy(module: KtModule): LLModuleResolutionStrategy {
        return resolutionStrategyProvider.getKind(module)
    }

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
}