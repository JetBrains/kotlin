/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api.lazy.resolve

import org.jetbrains.kotlin.fir.FirFakeSourceElementKind
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.psi
import org.jetbrains.kotlin.fir.render
import org.jetbrains.kotlin.fir.resolve.ResolutionMode
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.providers.FirProvider
import org.jetbrains.kotlin.fir.resolve.symbolProvider
import org.jetbrains.kotlin.fir.resolve.transformers.FirProviderInterceptorForSupertypeResolver
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.FirTowerDataContextCollector
import org.jetbrains.kotlin.idea.fir.low.level.api.api.FirDeclarationDesignation
import org.jetbrains.kotlin.idea.fir.low.level.api.api.FirDeclarationDesignationWithFile
import org.jetbrains.kotlin.idea.fir.low.level.api.api.collectDesignation
import org.jetbrains.kotlin.idea.fir.low.level.api.element.builder.getNonLocalContainingOrThisDeclaration
import org.jetbrains.kotlin.idea.fir.low.level.api.file.builder.FirFileBuilder
import org.jetbrains.kotlin.idea.fir.low.level.api.file.builder.ModuleFileCache
import org.jetbrains.kotlin.idea.fir.low.level.api.providers.firIdeProvider
import org.jetbrains.kotlin.idea.fir.low.level.api.transformers.*
import org.jetbrains.kotlin.idea.fir.low.level.api.util.*
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.containingClassOrObject

internal class FirLazyDeclarationResolver(
    private val firFileBuilder: FirFileBuilder
) {
    fun resolveFileAnnotations(
        firFile: FirFile,
        moduleFileCache: ModuleFileCache,
        scopeSession: ScopeSession,
    ) {
        check(firFile.resolvePhase >= FirResolvePhase.IMPORTS)
        firFileBuilder.runCustomResolveUnderLock(firFile, moduleFileCache) {
            val transformer = FirFileAnnotationsResolveTransformer(firFile.moduleData.session, scopeSession)
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
                .findSourceNonLocalFirDeclaration(firFileBuilder, declaration.moduleData.session.symbolProvider, moduleFileCache)
            return lazyResolveDeclaration(containingProperty, moduleFileCache, toPhase, towerDataContextCollector)
        }

        val firFile = declaration.getContainingFile()
            ?: error("FirFile was not found for\n${declaration.render()}")
        val provider = firFile.moduleData.session.firIdeProvider
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

    private fun calculateLazyBodies(firDeclaration: FirDeclaration, designation: FirDeclarationDesignation) {
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
        lastNonLazyPhase: FirResolvePhase = LAST_NON_LAZY_PHASE,
        firProviderInterceptor: FirProviderInterceptorForSupertypeResolver? = null
    ) {
        if (fromPhase >= toPhase) return
        val nonLazyPhase = minOf(toPhase, lastNonLazyPhase)

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
        val designation = nonLocalDeclarationToResolve.collectDesignation(containerFirFile)

        executeWithoutPCE {
            calculateLazyBodies(firDeclarationToResolve, designation)
        }

        var currentPhase = nonLazyPhase

        while (currentPhase < toPhase) {
            currentPhase = currentPhase.next
            if (currentPhase.pluginPhase) continue
            if (checkPCE) checkCanceled()
            runLazyResolvePhase(
                currentPhase,
                scopeSession,
                towerDataContextCollector,
                firProviderInterceptor,
                designation,
            )
        }
    }

    private fun runLazyResolvePhase(
        phase: FirResolvePhase,
        scopeSession: ScopeSession,
        towerDataContextCollector: FirTowerDataContextCollector?,
        firProviderInterceptor: FirProviderInterceptorForSupertypeResolver?,
        designation: FirDeclarationDesignationWithFile
    ) {
        if (designation.toSequence(includeTarget = true).all { it.resolvePhase >= phase }) {
            return
        }

        val transformer = phase.createLazyTransformer(
            designation,
            scopeSession,
            towerDataContextCollector,
            firProviderInterceptor,
        )

        firFileBuilder.firPhaseRunner.runPhaseWithCustomResolve(phase) {
            transformer.transformDeclaration()
        }
    }

    private fun FirResolvePhase.createLazyTransformer(
        designation: FirDeclarationDesignationWithFile,
        scopeSession: ScopeSession,
        towerDataContextCollector: FirTowerDataContextCollector?,
        firProviderInterceptor: FirProviderInterceptorForSupertypeResolver?,
    ): FirLazyTransformerForIDE = when (this) {
        FirResolvePhase.SUPER_TYPES -> FirDesignatedSupertypeResolverTransformerForIDE(
            designation,
            designation.firFile.moduleData.session,
            scopeSession,
            firProviderInterceptor,
        )
        FirResolvePhase.SEALED_CLASS_INHERITORS -> FirLazyTransformerForIDE.EMPTY
        FirResolvePhase.TYPES -> FirDesignatedTypeResolverTransformerForIDE(
            designation,
            designation.firFile.moduleData.session,
            scopeSession,
        )
        FirResolvePhase.STATUS -> FirDesignatedStatusResolveTransformerForIDE(
            designation,
            designation.firFile.moduleData.session,
            scopeSession
        )
        FirResolvePhase.CONTRACTS -> FirDesignatedContractsResolveTransformerForIDE(
            designation,
            designation.firFile.moduleData.session,
            scopeSession,
        )
        FirResolvePhase.IMPLICIT_TYPES_BODY_RESOLVE -> FirDesignatedImplicitTypesTransformerForIDE(
            designation,
            designation.firFile.moduleData.session,
            scopeSession
        )
        FirResolvePhase.BODY_RESOLVE -> FirDesignatedBodyResolveTransformerForIDE(
            designation,
            designation.firFile.moduleData.session,
            scopeSession,
            towerDataContextCollector,
            firProviderInterceptor
        )
        else -> error("Non-lazy phase $this")
    }

    private fun FirDeclaration.getNonLocalDeclarationToResolve(provider: FirProvider, moduleFileCache: ModuleFileCache): FirDeclaration {
        if (this is FirFile) return this
        val ktDeclaration = psi as? KtDeclaration ?: error("FirDeclaration should have a PSI of type KtDeclaration")
        if (declarationCanBeLazilyResolved(ktDeclaration)) return this
        val nonLocalPsi = ktDeclaration.getNonLocalContainingOrThisDeclaration()
            ?: error("Container for local declaration cannot be null")
        return nonLocalPsi.findSourceNonLocalFirDeclaration(firFileBuilder, provider.symbolProvider, moduleFileCache)
    }

    companion object {
        private val LAST_NON_LAZY_PHASE = FirResolvePhase.STATUS

        fun declarationCanBeLazilyResolved(declaration: KtDeclaration): Boolean {
            return when (declaration) {
                !is KtNamedDeclaration -> false
                is KtDestructuringDeclarationEntry, is KtFunctionLiteral, is KtTypeParameter -> false
                is KtPrimaryConstructor -> false
                is KtParameter -> {
                    if (declaration.hasValOrVar()) declaration.containingClassOrObject?.getClassId() != null
                    else false
                }
                is KtCallableDeclaration, is KtEnumEntry -> {
                    when (val parent = declaration.parent) {
                        is KtFile -> true
                        is KtClassBody -> (parent.parent as? KtClassOrObject)?.getClassId() != null
                        else -> false
                    }
                }
                is KtClassLikeDeclaration -> declaration.getClassId() != null
                else -> error("Unexpected ${declaration::class.qualifiedName}")
            }
        }
    }
}
