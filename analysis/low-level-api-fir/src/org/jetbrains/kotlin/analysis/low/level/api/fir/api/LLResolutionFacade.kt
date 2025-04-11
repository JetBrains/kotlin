/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.api

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.LLFirSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.state.LLDiagnosticProvider
import org.jetbrains.kotlin.analysis.low.level.api.fir.state.LLModuleProvider
import org.jetbrains.kotlin.analysis.low.level.api.fir.state.LLModuleResolutionStrategyProvider
import org.jetbrains.kotlin.analysis.low.level.api.fir.state.LLSessionProvider
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule
import org.jetbrains.kotlin.analysis.api.platform.projectStructure.KotlinProjectStructureProvider
import org.jetbrains.kotlin.analysis.api.utils.errors.withPsiEntry
import org.jetbrains.kotlin.analysis.low.level.api.fir.LLFirModuleResolveComponents
import org.jetbrains.kotlin.analysis.low.level.api.fir.element.builder.getNonLocalContainingOrThisElement
import org.jetbrains.kotlin.analysis.low.level.api.fir.state.LLDefaultScopeSessionProvider
import org.jetbrains.kotlin.analysis.low.level.api.fir.state.LLModuleResolutionStrategy
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.FirDeclarationForCompiledElementSearcher
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.errorWithFirSpecificEntries
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.findSourceNonLocalFirDeclaration
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.originalDeclaration
import org.jetbrains.kotlin.analysis.utils.errors.requireIsInstance
import org.jetbrains.kotlin.diagnostics.KtPsiDiagnostic
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.expressions.FirAnonymousFunctionExpression
import org.jetbrains.kotlin.fir.expressions.FirAnonymousObjectExpression
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.providers.firProvider
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhase
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.utils.exceptions.errorWithAttachment
import org.jetbrains.kotlin.utils.exceptions.requireWithAttachment

/**
 * An entry point for a FIR Low Level API resolution. Represents a project view from a use-site [KaModule].
 */
class LLResolutionFacade internal constructor(
    val moduleProvider: LLModuleProvider,
    val resolutionStrategyProvider: LLModuleResolutionStrategyProvider,
    val sessionProvider: LLSessionProvider,
    val diagnosticProvider: LLDiagnosticProvider,
) {
    val useSiteKtModule: KaModule
        get() = moduleProvider.useSiteModule

    val project: Project
        get() = useSiteKtModule.project

    val useSiteFirSession: LLFirSession
        get() = sessionProvider.useSiteSession

    fun getSessionFor(module: KaModule): LLFirSession {
        return sessionProvider.getSession(module)
    }

    fun getScopeSessionFor(firSession: FirSession): ScopeSession {
        requireIsInstance<LLFirSession>(firSession)
        return LLDefaultScopeSessionProvider.getScopeSession(firSession)
    }

    /**
     * Build [FirElement] node in its final resolved state for a requested element.
     *
     * Note: that it isn't always [BODY_RESOLVE][FirResolvePhase.BODY_RESOLVE]
     * as not all declarations have types/bodies/etc. to resolve.
     *
     * This operation could be time-consuming because it creates
     * [FileStructureElement][org.jetbrains.kotlin.analysis.low.level.api.fir.file.structure.FileStructureElement]
     * and may resolve non-local declarations into [BODY_RESOLVE][FirResolvePhase.BODY_RESOLVE] phase.
     *
     * Please use [getOrBuildFirFile] to get [FirFile] in undefined phase.
     *
     * @return associated [FirElement] in final resolved state if it exists.
     *
     * @see getOrBuildFirFile
     * @see org.jetbrains.kotlin.analysis.low.level.api.fir.element.builder.FirElementBuilder.getOrBuildFirFor
     */
    internal fun getOrBuildFirFor(element: KtElement): FirElement? {
        val moduleComponents = getModuleComponentsForElement(element)
        return moduleComponents.elementsBuilder.getOrBuildFirFor(element)
    }

    /**
     * Get or build or get cached [FirFile] for the requested file in undefined phase
     */
    internal fun getOrBuildFirFile(ktFile: KtFile): FirFile {
        val moduleComponents = getModuleComponentsForElement(ktFile)
        return moduleComponents.firFileBuilder.buildRawFirFileWithCaching(ktFile)
    }

    private fun getModuleComponentsForElement(element: KtElement): LLFirModuleResolveComponents {
        val module = getModule(element)
        return sessionProvider.getResolvableSession(module).moduleComponents
    }

    /**
     * @see LLDiagnosticProvider.getDiagnostics
     */
    internal fun getDiagnostics(element: KtElement, filter: DiagnosticCheckerFilter): List<KtPsiDiagnostic> {
        return diagnosticProvider.getDiagnostics(element, filter)
    }

    /**
     * @see LLDiagnosticProvider.collectDiagnostics
     */
    internal fun collectDiagnosticsForFile(ktFile: KtFile, filter: DiagnosticCheckerFilter): Collection<KtPsiDiagnostic> {
        return diagnosticProvider.collectDiagnostics(ktFile, filter)
    }

    internal fun resolveToFirSymbol(ktDeclaration: KtDeclaration, phase: FirResolvePhase): FirBasedSymbol<*> {
        val containingKtFile = ktDeclaration.containingKtFile
        val module = getModule(containingKtFile)

        return when (getModuleResolutionStrategy(module)) {
            LLModuleResolutionStrategy.LAZY -> findSourceFirSymbol(ktDeclaration).also { it.fir.lazyResolveToPhase(phase) }
            LLModuleResolutionStrategy.STATIC -> findCompiledFirSymbol(ktDeclaration, module)
        }
    }

    private fun getModuleResolutionStrategy(module: KaModule): LLModuleResolutionStrategy {
        return resolutionStrategyProvider.getKind(module)
    }

    private fun findSourceFirSymbol(ktDeclaration: KtDeclaration): FirBasedSymbol<*> {
        val targetDeclaration = ktDeclaration.originalDeclaration ?: ktDeclaration
        val targetModule = getModule(targetDeclaration)

        require(getModuleResolutionStrategy(targetModule) == LLModuleResolutionStrategy.LAZY) {
            "Declaration should be resolvable module, instead it had ${targetModule::class}"
        }

        val nonLocalContainer = targetDeclaration.getNonLocalContainingOrThisElement(codeFragmentAware = true)
            ?: errorWithAttachment("Declaration should have non-local container") {
                withPsiEntry("ktDeclaration", targetDeclaration, ::getModule)
                withEntry("module", targetModule) { it.moduleDescription }
            }

        val firDeclaration = if ((nonLocalContainer as? KtDeclaration) == targetDeclaration) {
            val session = sessionProvider.getResolvableSession(targetModule)
            nonLocalContainer.findSourceNonLocalFirDeclaration(
                firFileBuilder = session.moduleComponents.firFileBuilder,
                provider = session.firProvider,
            )
        } else {
            findSourceFirDeclarationViaResolve(targetDeclaration)
        }

        return firDeclaration.symbol
    }

    private fun findSourceFirDeclarationViaResolve(ktDeclaration: KtExpression): FirDeclaration {
        return when (val fir = getOrBuildFirFor(ktDeclaration)) {
            is FirDeclaration -> fir
            is FirAnonymousFunctionExpression -> fir.anonymousFunction
            is FirAnonymousObjectExpression -> fir.anonymousObject
            else -> errorWithFirSpecificEntries(
                "FirDeclaration was not found for ${ktDeclaration::class}, fir is ${fir?.let { it::class }}",
                fir = fir,
                psi = ktDeclaration,
            )
        }
    }

    private fun findCompiledFirSymbol(ktDeclaration: KtDeclaration, module: KaModule): FirBasedSymbol<*> {
        requireWithAttachment(
            ktDeclaration.containingKtFile.isCompiled,
            { "`findFirCompiledSymbol` only works on compiled declarations, but the given declaration is not compiled." },
        ) {
            withPsiEntry("declaration", ktDeclaration, module)
        }

        val session = getSessionFor(module)
        val searcher = FirDeclarationForCompiledElementSearcher(session)
        val firDeclaration = searcher.findNonLocalDeclaration(ktDeclaration)
        return firDeclaration.symbol
    }
}

fun LLResolutionFacade.getModule(element: PsiElement): KaModule {
    return KotlinProjectStructureProvider.getModule(project, element, useSiteKtModule)
}