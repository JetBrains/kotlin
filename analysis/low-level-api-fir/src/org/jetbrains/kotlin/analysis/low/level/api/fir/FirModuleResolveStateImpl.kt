/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.analysis.api.impl.barebone.annotations.InternalForInline
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.DiagnosticCheckerFilter
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.FirModuleResolveState
import org.jetbrains.kotlin.analysis.low.level.api.fir.diagnostics.DiagnosticsCollector
import org.jetbrains.kotlin.analysis.low.level.api.fir.element.builder.FirElementBuilder
import org.jetbrains.kotlin.analysis.low.level.api.fir.element.builder.getNonLocalContainingOrThisDeclaration
import org.jetbrains.kotlin.analysis.low.level.api.fir.file.builder.FirFileBuilder
import org.jetbrains.kotlin.analysis.low.level.api.fir.file.builder.ModuleFileCache
import org.jetbrains.kotlin.analysis.low.level.api.fir.file.structure.FileStructureCache
import org.jetbrains.kotlin.analysis.low.level.api.fir.lazy.resolve.FirLazyDeclarationResolver
import org.jetbrains.kotlin.analysis.low.level.api.fir.lazy.resolve.ResolveType
import org.jetbrains.kotlin.analysis.low.level.api.fir.lazy.resolve.lazyResolveDeclaration
import org.jetbrains.kotlin.analysis.low.level.api.fir.providers.firIdeProvider
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.FirIdeSessionProvider
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.FirIdeSourcesSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.FirDeclarationForCompiledElementSearcher
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.findSourceNonLocalFirDeclaration
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.getElementTextInContext
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.originalDeclaration
import org.jetbrains.kotlin.analysis.project.structure.KtModule
import org.jetbrains.kotlin.analysis.project.structure.KtSourceModule
import org.jetbrains.kotlin.analysis.project.structure.getKtModule
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.diagnostics.KtPsiDiagnostic
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.expressions.FirAnonymousFunctionExpression
import org.jetbrains.kotlin.fir.expressions.FirAnonymousObjectExpression
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.psi.*

internal class FirModuleResolveStateImpl(
    override val project: Project,
    override val module: KtModule,
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

    override fun getSessionFor(module: KtModule): FirSession =
        sessionProvider.getSession(module)!!

    override fun getOrBuildFirFor(element: KtElement): FirElement? =
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

    override fun getDiagnostics(element: KtElement, filter: DiagnosticCheckerFilter): List<KtPsiDiagnostic> =
        diagnosticsCollector.getDiagnosticsFor(element, filter)

    override fun collectDiagnosticsForFile(ktFile: KtFile, filter: DiagnosticCheckerFilter): Collection<KtPsiDiagnostic> =
        diagnosticsCollector.collectDiagnosticsForFile(ktFile, filter)

    @OptIn(InternalForInline::class)
    override fun findSourceFirDeclaration(ktDeclaration: KtDeclaration): FirDeclaration {
        return findSourceFirDeclarationByExpression(ktDeclaration.originalDeclaration ?: ktDeclaration)
    }

    @OptIn(InternalForInline::class)
    override fun findSourceFirDeclaration(ktDeclaration: KtLambdaExpression): FirDeclaration =
        findSourceFirDeclarationByExpression(ktDeclaration)

    /**
     * [ktDeclaration] should be either [KtDeclaration] or [KtLambdaExpression]
     */
    private fun findSourceFirDeclarationByExpression(ktDeclaration: KtExpression): FirDeclaration {
        val module = ktDeclaration.getKtModule(project)
        require(module is KtSourceModule) {
            "Declaration should have ModuleSourceInfo, instead it had ${module::class}"
        }
        val nonLocalNamedDeclaration = ktDeclaration.getNonLocalContainingOrThisDeclaration()
            ?: error("Declaration should have non-local container${ktDeclaration.getElementTextInContext()}")

        if (ktDeclaration == nonLocalNamedDeclaration) {
            return nonLocalNamedDeclaration.findSourceNonLocalFirDeclaration(
                firFileBuilder = firFileBuilder,
                firSymbolProvider = rootModuleSession.firIdeProvider.symbolProvider,
                moduleFileCache = sessionProvider.getModuleCache(module)
            )
        }

        return when (val localFirElement = getOrBuildFirFor(ktDeclaration)) {
            is FirDeclaration -> localFirElement
            is FirAnonymousFunctionExpression -> localFirElement.anonymousFunction
            is FirAnonymousObjectExpression -> localFirElement.anonymousObject
            else -> error("FirDeclaration was not found for\n${ktDeclaration.getElementTextInContext()}")
        }
    }

    @OptIn(InternalForInline::class)
    override fun findSourceFirCompiledDeclaration(ktDeclaration: KtDeclaration): FirDeclaration {
        require(ktDeclaration.containingKtFile.isCompiled) {
            "This method will only work on compiled declarations, but this declaration is not compiled: ${ktDeclaration.getElementTextInContext()}"
        }

        val searcher = FirDeclarationForCompiledElementSearcher(rootModuleSession.symbolProvider)

        return when (ktDeclaration) {
            is KtEnumEntry -> searcher.findNonLocalEnumEntry(ktDeclaration)
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
        return firLazyDeclarationResolver.lazyResolveDeclaration(
            firDeclarationToResolve = declaration,
            moduleFileCache = fileCache,
            scopeSession = ScopeSession(),
            toPhase = toPhase,
            checkPCE = true,
        )
    }

    override fun <D : FirDeclaration> resolveFirToResolveType(declaration: D, type: ResolveType): D {
        if (type == ResolveType.NoResolve) return declaration
        val fileCache = when (val session = declaration.moduleData.session) {
            is FirIdeSourcesSession -> session.cache
            else -> return declaration
        }
        return firLazyDeclarationResolver.lazyResolveDeclaration(
            firDeclaration = declaration,
            moduleFileCache = fileCache,
            scopeSession = ScopeSession(),
            toResolveType = type,
            checkPCE = true,
        )
    }
}
