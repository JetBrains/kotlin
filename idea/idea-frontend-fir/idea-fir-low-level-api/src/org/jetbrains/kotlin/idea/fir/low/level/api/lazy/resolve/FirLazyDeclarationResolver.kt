/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api.lazy.resolve

import org.jetbrains.kotlin.fir.FirFakeSourceElementKind
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.psi
import org.jetbrains.kotlin.fir.render
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.providers.FirProvider
import org.jetbrains.kotlin.fir.resolve.symbolProvider
import org.jetbrains.kotlin.fir.resolve.transformers.FirProviderInterceptor
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.FirTowerDataContextCollector
import org.jetbrains.kotlin.idea.fir.low.level.api.api.FirDeclarationUntypedDesignation
import org.jetbrains.kotlin.idea.fir.low.level.api.api.FirDeclarationUntypedDesignationWithFile
import org.jetbrains.kotlin.idea.fir.low.level.api.api.collectDesignation
import org.jetbrains.kotlin.idea.fir.low.level.api.element.builder.getNonLocalContainingOrThisDeclaration
import org.jetbrains.kotlin.idea.fir.low.level.api.file.builder.FirFileBuilder
import org.jetbrains.kotlin.idea.fir.low.level.api.file.builder.ModuleFileCache
import org.jetbrains.kotlin.idea.fir.low.level.api.providers.firIdeProvider
import org.jetbrains.kotlin.idea.fir.low.level.api.transformers.*
import org.jetbrains.kotlin.idea.fir.low.level.api.util.*
import org.jetbrains.kotlin.idea.util.ifTrue
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.containingClassOrObject

internal class FirLazyDeclarationResolver(
    private val firFileBuilder: FirFileBuilder
) {
    /**
     * Fully resolve file annotations (synchronized)
     * @see resolveFileAnnotationsWithoutLock not synchronized
     */
    fun resolveFileAnnotations(
        firFile: FirFile,
        annotations: List<FirAnnotationCall>,
        moduleFileCache: ModuleFileCache,
        scopeSession: ScopeSession,
    ) {
        lazyResolveDeclaration(
            declaration = firFile,
            moduleFileCache = moduleFileCache,
            toPhase = FirResolvePhase.IMPORTS,
            checkPCE = false,
            reresolveFile = false
        )
        firFileBuilder.runCustomResolveUnderLock(firFile, moduleFileCache) {
            resolveFileAnnotationsWithoutLock(firFile, annotations, scopeSession)
        }
    }

    /**
     * Fully resolve file annotations (not synchronized)
     * @see resolveFileAnnotations synchronized version
     */
    private fun resolveFileAnnotationsWithoutLock(
        firFile: FirFile,
        annotations: List<FirAnnotationCall>,
        scopeSession: ScopeSession,
    ) {
        FirFileAnnotationsResolveTransformer(
            firFile = firFile,
            annotations = annotations,
            session = firFile.moduleData.session,
            scopeSession = scopeSession
        ).transformDeclaration()
    }

    private fun getResolvableDeclaration(declaration: FirDeclaration, moduleFileCache: ModuleFileCache): FirDeclaration {
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
            return ktContainingResolvableDeclaration.findSourceNonLocalFirDeclaration(
                firFileBuilder = firFileBuilder,
                firSymbolProvider = declaration.moduleData.session.symbolProvider,
                moduleFileCache = moduleFileCache
            )
        }

        return declaration
    }

    /**
     * Run partially designated resolve that resolve declaration into last file-wise resolve and then resolve a designation (synchronized)
     * @see LAST_NON_LAZY_PHASE is the last file-wise resolve
     * @see lazyDesignatedResolveDeclaration designated resolve
     * @see runLazyResolveWithoutLock (not synchronized)
     */
    fun lazyResolveDeclaration(
        declaration: FirDeclaration,
        moduleFileCache: ModuleFileCache,
        toPhase: FirResolvePhase,
        checkPCE: Boolean = false,
        reresolveFile: Boolean = false,
    ) {
        if (declaration.resolvePhase >= toPhase) return

        val resolvableDeclaration = getResolvableDeclaration(declaration, moduleFileCache)

        val firFile = resolvableDeclaration.getContainingFile()
            ?: error("FirFile was not found for\n${resolvableDeclaration.render()}")
        val provider = firFile.moduleData.session.firIdeProvider

        // Lazy since we want to read the resolve phase inside the lock. Otherwise, we may run the same resolve phase multiple times. See
        // KT-45121
        val fromPhase: FirResolvePhase by lazy(LazyThreadSafetyMode.NONE) {
            if (reresolveFile) resolvableDeclaration.resolvePhase else minOf(firFile.resolvePhase, resolvableDeclaration.resolvePhase)
        }

        if (checkPCE) {
            firFileBuilder.runCustomResolveWithPCECheck(firFile, moduleFileCache) {
                runLazyResolveWithoutLock(
                    firDeclarationToResolve = declaration,
                    moduleFileCache = moduleFileCache,
                    containerFirFile = firFile,
                    provider = provider,
                    fromPhase = fromPhase,
                    toPhase = toPhase,
                    checkPCE = true,
                )
            }
        } else {
            firFileBuilder.runCustomResolveUnderLock(firFile, moduleFileCache) {
                executeWithoutPCE {
                    runLazyResolveWithoutLock(
                        firDeclarationToResolve = declaration,
                        moduleFileCache = moduleFileCache,
                        containerFirFile = firFile,
                        provider = provider,
                        fromPhase = fromPhase,
                        toPhase = toPhase,
                        checkPCE = false,
                    )
                }
            }
        }
    }

    private fun createLazyBodiesCalculator(designation: FirDeclarationUntypedDesignation): (FirResolvePhase) -> Unit {
        var calculated = false
        return { phase: FirResolvePhase ->
            if (!calculated && phase >= FirResolvePhase.CONTRACTS) {
                executeWithoutPCE {
                    FirLazyBodiesCalculator.calculateLazyBodiesInside(designation)
                }
                calculated = true
            }
        }
    }

    /**
     * Designated resolve (not synchronized)
     * @see runLazyDesignatedResolveWithoutLock for designated resolve
     * @see lazyResolveDeclaration synchronized version
     */
    private fun runLazyResolveWithoutLock(
        firDeclarationToResolve: FirDeclaration,
        moduleFileCache: ModuleFileCache,
        containerFirFile: FirFile,
        provider: FirProvider,
        fromPhase: FirResolvePhase,
        toPhase: FirResolvePhase,
        checkPCE: Boolean,
    ) {
        if (fromPhase >= toPhase) return
        val nonLazyPhase = minOf(toPhase, LAST_NON_LAZY_PHASE)

        val scopeSession = ScopeSession()
        if (fromPhase < nonLazyPhase) {
            firFileBuilder.runResolveWithoutLock(
                firFile = containerFirFile,
                fromPhase = fromPhase,
                toPhase = nonLazyPhase,
                scopeSession = scopeSession,
                checkPCE = checkPCE
            )
        }
        if (toPhase <= nonLazyPhase) return
        resolveFileAnnotationsWithoutLock(containerFirFile, containerFirFile.annotations, scopeSession)

        runLazyDesignatedResolveWithoutLock(
            firDeclarationToResolve = firDeclarationToResolve,
            moduleFileCache = moduleFileCache,
            containerFirFile = containerFirFile,
            provider = provider,
            fromPhase = LAST_NON_LAZY_PHASE,
            toPhase = toPhase,
            checkPCE = checkPCE,
            isOnAirResolve = false
        )
    }

    /**
     * Run designated resolve only designation with fully resolved path (synchronized).
     * Suitable for body resolve or/and on-air resolve.
     * @see lazyResolveDeclaration for ordinary resolve
     * @param firDeclarationToResolve target non-local declaration
     * @param isOnAirResolve should be true when node does not belong to it's true designation (OnAir resolve in custom context)
     */
    fun lazyDesignatedResolveDeclaration(
        firDeclarationToResolve: FirDeclaration,
        moduleFileCache: ModuleFileCache,
        containerFirFile: FirFile,
        provider: FirProvider,
        toPhase: FirResolvePhase,
        checkPCE: Boolean,
        isOnAirResolve: Boolean,
        towerDataContextCollector: FirTowerDataContextCollector? = null,
    ) {
        // Lazy since we want to read the resolve phase inside the lock. Otherwise, we may run the same resolve phase multiple times. See
        // KT-45121
        val fromPhase: FirResolvePhase by lazy(LazyThreadSafetyMode.NONE) {
            minOf(containerFirFile.resolvePhase, firDeclarationToResolve.resolvePhase)
        }

        if (checkPCE) {
            firFileBuilder.runCustomResolveWithPCECheck(containerFirFile, moduleFileCache) {
                runLazyDesignatedResolveWithoutLock(
                    firDeclarationToResolve = firDeclarationToResolve,
                    moduleFileCache = moduleFileCache,
                    containerFirFile = containerFirFile,
                    provider = provider,
                    fromPhase = fromPhase,
                    toPhase = toPhase,
                    checkPCE = checkPCE,
                    isOnAirResolve = isOnAirResolve,
                    towerDataContextCollector = towerDataContextCollector,
                )
            }
        } else {
            firFileBuilder.runCustomResolveUnderLock(containerFirFile, moduleFileCache) {
                runLazyDesignatedResolveWithoutLock(
                    firDeclarationToResolve = firDeclarationToResolve,
                    moduleFileCache = moduleFileCache,
                    containerFirFile = containerFirFile,
                    provider = provider,
                    fromPhase = fromPhase,
                    toPhase = toPhase,
                    checkPCE = checkPCE,
                    isOnAirResolve = isOnAirResolve,
                    towerDataContextCollector = towerDataContextCollector,
                )
            }
        }
    }

    /**
     * Designated resolve (not synchronized)
     * @see runLazyResolveWithoutLock for ordinary resolve
     * @see lazyDesignatedResolveDeclaration synchronized version
     */
    private fun runLazyDesignatedResolveWithoutLock(
        firDeclarationToResolve: FirDeclaration,
        moduleFileCache: ModuleFileCache,
        containerFirFile: FirFile,
        provider: FirProvider,
        fromPhase: FirResolvePhase,
        toPhase: FirResolvePhase,
        checkPCE: Boolean,
        isOnAirResolve: Boolean,
        towerDataContextCollector: FirTowerDataContextCollector? = null
    ) {
        var currentPhase = fromPhase
        runLazyResolveWithoutLock(
            firDeclarationToResolve = firDeclarationToResolve,
            moduleFileCache = moduleFileCache,
            containerFirFile = containerFirFile,
            provider = provider,
            fromPhase = currentPhase,
            toPhase = FirResolvePhase.IMPORTS,
            checkPCE = checkPCE
        )
        currentPhase = maxOf(fromPhase, FirResolvePhase.IMPORTS)
        if (currentPhase >= toPhase) return

        val nonLocalDeclarationToResolve = firDeclarationToResolve.getNonLocalDeclarationToResolve(provider, moduleFileCache)
        val designation = nonLocalDeclarationToResolve.collectDesignation(containerFirFile)
        check(!designation.isLocalDesignation) { "Could not resolve local designation" }

        val lazyBodiesCalculator = createLazyBodiesCalculator(designation)
        val scopeSession = ScopeSession()

        //This needed to override standard symbol resolve in supertype transformer with adding on-air created symbols
        val firProviderInterceptor = isOnAirResolve.ifTrue {
            FirProviderInterceptorForIDE.createForFirElement(
                session = designation.firFile.moduleData.session,
                firFile = designation.firFile,
                element = designation.declaration
            )
        }

        while (currentPhase < toPhase) {
            currentPhase = currentPhase.next
            if (currentPhase.pluginPhase) continue
            if (checkPCE) checkCanceled()
            lazyBodiesCalculator(currentPhase)
            runLazyResolvePhase(
                phase = currentPhase,
                scopeSession = scopeSession,
                designation = designation,
                towerDataContextCollector = towerDataContextCollector,
                firProviderInterceptor = firProviderInterceptor
            )
        }
    }

    private fun runLazyResolvePhase(
        phase: FirResolvePhase,
        scopeSession: ScopeSession,
        designation: FirDeclarationUntypedDesignationWithFile,
        towerDataContextCollector: FirTowerDataContextCollector?,
        firProviderInterceptor: FirProviderInterceptor?
    ) {
        if (designation.declaration.resolvePhase >= phase) return

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
        designation: FirDeclarationUntypedDesignationWithFile,
        scopeSession: ScopeSession,
        towerDataContextCollector: FirTowerDataContextCollector?,
        firProviderInterceptor: FirProviderInterceptor?,
    ): FirLazyTransformerForIDE = when (this) {
        FirResolvePhase.SUPER_TYPES -> FirDesignatedSupertypeResolverTransformerForIDE(
            designation,
            designation.firFile.moduleData.session,
            scopeSession,
            firProviderInterceptor,
        )
        FirResolvePhase.SEALED_CLASS_INHERITORS -> FirLazyTransformerForIDE.DUMMY
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
            scopeSession,
            towerDataContextCollector
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
