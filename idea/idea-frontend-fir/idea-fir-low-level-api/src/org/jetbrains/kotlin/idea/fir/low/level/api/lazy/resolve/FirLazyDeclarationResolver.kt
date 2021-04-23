/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api.lazy.resolve

import com.intellij.psi.util.parentsOfType
import org.jetbrains.kotlin.fir.FirFakeSourceElementKind
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.psi
import org.jetbrains.kotlin.fir.render
import org.jetbrains.kotlin.fir.resolve.ResolutionMode
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.providers.FirProvider
import org.jetbrains.kotlin.fir.resolve.symbolProvider
import org.jetbrains.kotlin.idea.fir.low.level.api.element.builder.FirTowerDataContextCollector
import org.jetbrains.kotlin.idea.fir.low.level.api.element.builder.getNonLocalContainingOrThisDeclaration
import org.jetbrains.kotlin.idea.fir.low.level.api.file.builder.FirFileBuilder
import org.jetbrains.kotlin.idea.fir.low.level.api.file.builder.ModuleFileCache
import org.jetbrains.kotlin.idea.fir.low.level.api.providers.firIdeProvider
import org.jetbrains.kotlin.idea.fir.low.level.api.trasformers.*
import org.jetbrains.kotlin.idea.fir.low.level.api.trasformers.FirDesignatedContractsResolveTransformerForIDE
import org.jetbrains.kotlin.idea.fir.low.level.api.trasformers.FirDesignatedImplicitTypesTransformerForIDE
import org.jetbrains.kotlin.idea.fir.low.level.api.trasformers.FirFileAnnotationsResolveTransformer
import org.jetbrains.kotlin.idea.fir.low.level.api.util.*
import org.jetbrains.kotlin.psi.*

internal class FirLazyDeclarationResolver(
    private val firFileBuilder: FirFileBuilder
) {
    fun resolveFileAnnotations(
        firFile: FirFile,
        moduleFileCache: ModuleFileCache,
        scopeSession: ScopeSession,
    ) {
        firFileBuilder.runCustomResolveUnderLock(firFile, moduleFileCache) {
            val transformer = FirFileAnnotationsResolveTransformer(firFile.declarationSiteSession, scopeSession)
            firFile.accept(transformer, ResolutionMode.ContextDependent)
        }
    }

    fun lazyResolveDeclaration(
        declaration: FirDeclaration,
        moduleFileCache: ModuleFileCache,
        toPhase: FirResolvePhase,
        towerDataContextCollector: FirTowerDataContextCollector? = null,
        checkPCE: Boolean = false,
        reresolveFile: Boolean = false,
    ) {
        if (declaration.resolvePhase >= toPhase) return

        if (declaration is FirPropertyAccessor || declaration is FirTypeParameter || declaration is FirValueParameter) {
            val ktContainingResolvableDeclaration = when (val psi = declaration.psi) {
                is KtPropertyAccessor -> psi.property
                is KtProperty -> psi
                is KtParameter, is KtTypeParameter -> psi.getNonLocalContainingOrThisDeclaration()
                    ?: error("Cannot find containing declaration for KtParameter")
                is KtCallExpression -> {
                    check(declaration.source?.kind == FirFakeSourceElementKind.DefaultAccessor)
                    val delegationCall = psi.parent as KtPropertyDelegate
                    delegationCall.parent as KtProperty
                }
                null -> error("Cannot find containing declaration for KtParameter")
                else -> error("Invalid source of property accessor ${psi::class}")
            }
            val containingProperty = ktContainingResolvableDeclaration
                .findSourceNonLocalFirDeclaration(firFileBuilder, declaration.declarationSiteSession.symbolProvider, moduleFileCache)
            return lazyResolveDeclaration(containingProperty, moduleFileCache, toPhase, towerDataContextCollector)
        }

        val firFile = declaration.getContainingFile()
            ?: error("FirFile was not found for\n${declaration.render()}")
        val provider = firFile.declarationSiteSession.firIdeProvider
        // Lazy since we want to read the resolve phase inside the lock. Otherwise, we may run the same resolve phase multiple times. See
        // KT-45121
        val fromPhase: FirResolvePhase by lazy(LazyThreadSafetyMode.NONE) {
            if (reresolveFile) declaration.resolvePhase else minOf(firFile.resolvePhase, declaration.resolvePhase)
        }

        if (checkPCE) {
            firFileBuilder.runCustomResolveWithPCECheck(firFile, moduleFileCache) {
                runLazyResolveWithoutLock(
                    declaration,
                    moduleFileCache,
                    firFile,
                    provider,
                    fromPhase,
                    toPhase,
                    towerDataContextCollector,
                    checkPCE = true
                )
            }
        } else {
            firFileBuilder.runCustomResolveUnderLock(firFile, moduleFileCache) {
                executeWithoutPCE {
                    runLazyResolveWithoutLock(
                        declaration,
                        moduleFileCache,
                        firFile,
                        provider,
                        fromPhase,
                        toPhase,
                        towerDataContextCollector,
                        checkPCE = false
                    )
                }
            }
        }
    }

    private fun calculateLazyBodies(firDeclaration: FirDeclaration, designation: List<FirDeclaration>) {
        FirLazyBodiesCalculator.calculateLazyBodiesInside(firDeclaration, designation)
    }

    fun runLazyResolveWithoutLock(
        firDeclarationToResolve: FirDeclaration,
        moduleFileCache: ModuleFileCache,
        containerFirFile: FirFile,
        provider: FirProvider,
        fromPhase: FirResolvePhase,
        toPhase: FirResolvePhase,
        towerDataContextCollector: FirTowerDataContextCollector? = null,
        checkPCE: Boolean,
    ) {
        if (fromPhase >= toPhase) return
        val nonLazyPhase = minOf(toPhase, LAST_NON_LAZY_PHASE)

        val scopeSession = ScopeSession()
        if (fromPhase < nonLazyPhase) {
            firFileBuilder.runResolveWithoutLock(
                containerFirFile,
                fromPhase = fromPhase,
                toPhase = nonLazyPhase,
                scopeSession = scopeSession,
                checkPCE = checkPCE
            )
        }
        if (toPhase <= nonLazyPhase) return
        resolveFileAnnotations(containerFirFile, moduleFileCache, scopeSession)

        val nonLocalDeclarationToResolve = firDeclarationToResolve.getNonLocalDeclarationToResolve(provider, moduleFileCache)
        val designation = nonLocalDeclarationToResolve.getDesignation(containerFirFile, provider, moduleFileCache)

        executeWithoutPCE {
            calculateLazyBodies(firDeclarationToResolve, designation)
        }

        var currentPhase = nonLazyPhase

        while (currentPhase < toPhase) {
            currentPhase = currentPhase.next
            if (currentPhase.pluginPhase) continue
            if (checkPCE) checkCanceled()
            runLazyResolvePhase(
                containerFirFile,
                currentPhase,
                scopeSession,
                towerDataContextCollector,
                designation
            )
        }
    }

    private fun runLazyResolvePhase(
        containerFirFile: FirFile,
        phase: FirResolvePhase,
        scopeSession: ScopeSession,
        towerDataContextCollector: FirTowerDataContextCollector?,
        designation: List<FirDeclaration>
    ) {
        if (designation.all { it.resolvePhase >= phase }) {
            return
        }

        val transformer = phase.createLazyTransformer(
            designation,
            containerFirFile,
            scopeSession,
            towerDataContextCollector
        )

        firFileBuilder.firPhaseRunner.runPhaseWithCustomResolve(phase) {
            containerFirFile.transform<FirFile, ResolutionMode>(transformer, ResolutionMode.ContextIndependent)
        }
    }

    private fun FirResolvePhase.createLazyTransformer(
        designation: List<FirDeclaration>,
        containerFirFile: FirFile,
        scopeSession: ScopeSession,
        towerDataContextCollector: FirTowerDataContextCollector?
    ) = when (this) {
        FirResolvePhase.CONTRACTS -> FirDesignatedContractsResolveTransformerForIDE(
            FirDesignation(designation),
            containerFirFile.declarationSiteSession,
            scopeSession,
        )
        FirResolvePhase.IMPLICIT_TYPES_BODY_RESOLVE -> FirDesignatedImplicitTypesTransformerForIDE(
            FirDesignation(designation),
            containerFirFile.declarationSiteSession,
            scopeSession,
            towerDataContextCollector,
        )
        FirResolvePhase.BODY_RESOLVE -> FirDesignatedBodyResolveTransformerForIDE(
            FirDesignation(designation),
            containerFirFile.declarationSiteSession,
            scopeSession,
            towerDataContextCollector
        )
        else -> error("Non-lazy phase $this")
    }

    @OptIn(ExperimentalStdlibApi::class)
    private fun FirDeclaration.getDesignation(
        containerFirFile: FirFile,
        provider: FirProvider,
        moduleFileCache: ModuleFileCache
    ): List<FirDeclaration> = buildList {
        if (this !is FirFile) {
            val ktDeclaration = ktDeclaration
            ktDeclaration.parentsOfType<KtClassOrObject>(withSelf = true)
                .filter { it !is KtEnumEntry }
                .map { it.findSourceNonLocalFirDeclaration(firFileBuilder, provider.symbolProvider, moduleFileCache, containerFirFile) }
                .toList()
                .asReversed()
                .let(::addAll)
            if (this@getDesignation is FirCallableDeclaration<*> || this@getDesignation is FirTypeAlias) {
                add(this@getDesignation)
            }
        }
    }


    private fun FirDeclaration.getNonLocalDeclarationToResolve(provider: FirProvider, moduleFileCache: ModuleFileCache): FirDeclaration {
        if (this is FirFile) return this
        val ktDeclaration = psi as? KtDeclaration ?: error("FirDeclaration should have a PSI of type KtDeclaration")
        if (!KtPsiUtil.isLocal(ktDeclaration)) return this
        val nonLocalPsi = ktDeclaration.getNonLocalContainingOrThisDeclaration()
            ?: error("Container for local declaration cannot be null")
        return nonLocalPsi.findSourceNonLocalFirDeclaration(firFileBuilder, provider.symbolProvider, moduleFileCache)
    }

    companion object {
        private val LAST_NON_LAZY_PHASE = FirResolvePhase.STATUS
    }
}
