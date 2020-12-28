/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api.lazy.resolve

import com.intellij.psi.util.parentsOfType
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.psi
import org.jetbrains.kotlin.fir.render
import org.jetbrains.kotlin.fir.resolve.ResolutionMode
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.firSymbolProvider
import org.jetbrains.kotlin.fir.resolve.providers.FirProvider
import org.jetbrains.kotlin.idea.fir.low.level.api.element.builder.FirTowerDataContextCollector
import org.jetbrains.kotlin.idea.fir.low.level.api.trasformers.FirDesignatedBodyResolveTransformerForIDE
import org.jetbrains.kotlin.idea.fir.low.level.api.element.builder.getNonLocalContainingOrThisDeclaration
import org.jetbrains.kotlin.idea.fir.low.level.api.file.builder.FirFileBuilder
import org.jetbrains.kotlin.idea.fir.low.level.api.file.builder.ModuleFileCache
import org.jetbrains.kotlin.idea.fir.low.level.api.providers.firIdeProvider
import org.jetbrains.kotlin.idea.fir.low.level.api.trasformers.FirDesignatedContractsResolveTransformerForIDE
import org.jetbrains.kotlin.idea.fir.low.level.api.trasformers.FirDesignatedImplicitTypesTransformerForIDE
import org.jetbrains.kotlin.idea.fir.low.level.api.util.*
import org.jetbrains.kotlin.idea.fir.low.level.api.util.checkCanceled
import org.jetbrains.kotlin.idea.fir.low.level.api.util.executeWithoutPCE
import org.jetbrains.kotlin.idea.fir.low.level.api.util.findSourceNonLocalFirDeclaration
import org.jetbrains.kotlin.psi.*

internal class FirLazyDeclarationResolver(
    private val firFileBuilder: FirFileBuilder
) {
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
            val ktContainingResolvableDeclaration = when (val ktDeclaration = declaration.ktDeclaration) {
                is KtPropertyAccessor -> ktDeclaration.property
                is KtProperty -> ktDeclaration
                is KtParameter, is KtTypeParameter -> ktDeclaration.getNonLocalContainingOrThisDeclaration()
                    ?: error("Cannot find containing declaration for KtParameter")
                else -> error("Invalid source of property accessor ${ktDeclaration::class}")
            }
            val containingProperty = ktContainingResolvableDeclaration
                .findSourceNonLocalFirDeclaration(firFileBuilder, declaration.session.firSymbolProvider, moduleFileCache)
            return lazyResolveDeclaration(containingProperty, moduleFileCache, toPhase, towerDataContextCollector)
        }

        val firFile = declaration.getContainingFile()
            ?: error("FirFile was not found for\n${declaration.render()}")
        val provider = firFile.session.firIdeProvider
        val fromPhase = if (reresolveFile) declaration.resolvePhase else minOf(firFile.resolvePhase, declaration.resolvePhase)

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

    private fun calculateLazyBodies(firDeclaration: FirDeclaration) {
        FirLazyBodiesCalculator.calculateLazyBodiesInside(firDeclaration)
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
        if (fromPhase < nonLazyPhase) {
            firFileBuilder.runResolveWithoutLock(
                containerFirFile,
                fromPhase = fromPhase,
                toPhase = nonLazyPhase,
                checkPCE = checkPCE
            )
        }
        if (toPhase <= nonLazyPhase) return

        executeWithoutPCE {
            calculateLazyBodies(firDeclarationToResolve)
        }

        var currentPhase = nonLazyPhase
        val scopeSession = ScopeSession()

        while (currentPhase < toPhase) {
            currentPhase = currentPhase.next
            if (currentPhase.pluginPhase) continue
            if (checkPCE) checkCanceled()
            runLazyResolvePhase(
                firDeclarationToResolve,
                containerFirFile,
                moduleFileCache,
                provider,
                currentPhase,
                scopeSession,
                towerDataContextCollector
            )
        }
    }

    private fun runLazyResolvePhase(
        firDeclarationToResolve: FirDeclaration,
        containerFirFile: FirFile,
        moduleFileCache: ModuleFileCache,
        provider: FirProvider,
        phase: FirResolvePhase,
        scopeSession: ScopeSession,
        towerDataContextCollector: FirTowerDataContextCollector?,
    ) {
        val nonLocalDeclarationToResolve = firDeclarationToResolve.getNonLocalDeclarationToResolve(provider, moduleFileCache)

        val designation = nonLocalDeclarationToResolve.getDesignation(containerFirFile, provider, moduleFileCache)

        if (designation.all { it.resolvePhase >= phase }) {
            return
        }

        val transformer = phase.createLazyTransformer(
            designation,
            firDeclarationToResolve,
            containerFirFile,
            scopeSession,
            towerDataContextCollector
        )

        firFileBuilder.firPhaseRunner.runPhaseWithCustomResolve(phase) {
            containerFirFile.transform<FirFile, ResolutionMode>(transformer, ResolutionMode.ContextDependent)
        }
    }

    private fun FirResolvePhase.createLazyTransformer(
        designation: List<FirDeclaration>,
        targetDeclaration: FirDeclaration,
        containerFirFile: FirFile,
        scopeSession: ScopeSession,
        towerDataContextCollector: FirTowerDataContextCollector?
    ) = when (this) {
        FirResolvePhase.CONTRACTS -> FirDesignatedContractsResolveTransformerForIDE(
            designation.iterator(),
            targetDeclaration,
            containerFirFile.session,
            scopeSession,
        )
        FirResolvePhase.IMPLICIT_TYPES_BODY_RESOLVE -> FirDesignatedImplicitTypesTransformerForIDE(
            designation.iterator(),
            targetDeclaration,
            containerFirFile.session,
            scopeSession,
            towerDataContextCollector,
        )
        FirResolvePhase.BODY_RESOLVE -> FirDesignatedBodyResolveTransformerForIDE(
            designation.iterator(),
            targetDeclaration,
            containerFirFile.session,
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
            if (this@getDesignation is FirCallableDeclaration<*>) {
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


