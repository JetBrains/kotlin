/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api.lazy.resolve

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.transformers.FirImportResolveTransformer
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.FirTowerDataContextCollector
import org.jetbrains.kotlin.idea.fir.low.level.api.api.FirDeclarationDesignationWithFile
import org.jetbrains.kotlin.idea.fir.low.level.api.file.builder.FirFileBuilder
import org.jetbrains.kotlin.idea.fir.low.level.api.file.builder.ModuleFileCache
import org.jetbrains.kotlin.idea.fir.low.level.api.file.builder.runCustomResolveUnderLock
import org.jetbrains.kotlin.idea.fir.low.level.api.providers.firIdeProvider
import org.jetbrains.kotlin.idea.fir.low.level.api.transformers.FirFileAnnotationsResolveTransformer
import org.jetbrains.kotlin.idea.fir.low.level.api.transformers.FirLazyTransformerForIDE.Companion.resolvePhaseForAllDeclarations
import org.jetbrains.kotlin.idea.fir.low.level.api.transformers.FirProviderInterceptorForIDE
import org.jetbrains.kotlin.idea.fir.low.level.api.transformers.LazyTransformerFactory
import org.jetbrains.kotlin.idea.fir.low.level.api.util.checkCanceled
import org.jetbrains.kotlin.idea.fir.low.level.api.util.ensurePhase
import org.jetbrains.kotlin.idea.util.ifFalse
import org.jetbrains.kotlin.idea.util.ifTrue

internal class FirLazyDeclarationResolver(private val firFileBuilder: FirFileBuilder) {
    /**
     * Fully resolve file annotations (synchronized)
     * @see resolveFileAnnotationsWithoutLock not synchronized
     */
    fun resolveFileAnnotations(
        firFile: FirFile,
        annotations: List<FirAnnotationCall>,
        moduleFileCache: ModuleFileCache,
        scopeSession: ScopeSession,
        checkPCE: Boolean,
        collector: FirTowerDataContextCollector? = null,
    ) {
        moduleFileCache.firFileLockProvider.runCustomResolveUnderLock(firFile, checkPCE) {
            resolveFileAnnotationsWithoutLock(
                firFile = firFile,
                moduleFileCache = moduleFileCache,
                annotations = annotations,
                scopeSession = scopeSession,
                collector = collector
            )
        }
    }

    /**
     * Fully resolve file annotations (not synchronized)
     * @see resolveFileAnnotations synchronized version
     */
    private fun resolveFileAnnotationsWithoutLock(
        firFile: FirFile,
        moduleFileCache: ModuleFileCache,
        annotations: List<FirAnnotationCall>,
        scopeSession: ScopeSession,
        collector: FirTowerDataContextCollector? = null,
    ) {
        lazyResolveFileDeclarationWithoutLock(
            firFile = firFile,
            moduleFileCache = moduleFileCache,
            toPhase = FirResolvePhase.IMPORTS,
            scopeSession = scopeSession,
            checkPCE = false,
            collector = collector
        )

        FirFileAnnotationsResolveTransformer(
            firFile = firFile,
            annotations = annotations,
            session = firFile.moduleData.session,
            scopeSession = scopeSession,
            firTowerDataContextCollector = collector,
        ).transformDeclaration(firFileBuilder.firPhaseRunner)
    }

    private fun FirDeclaration.isValidForResolve(): Boolean = when (origin) {
        is FirDeclarationOrigin.Source,
        is FirDeclarationOrigin.ImportedFromObject,
        is FirDeclarationOrigin.Delegated,
        is FirDeclarationOrigin.Synthetic,
        is FirDeclarationOrigin.SubstitutionOverride,
        is FirDeclarationOrigin.IntersectionOverride -> {
            when (this) {
                is FirFile -> true
                is FirSimpleFunction,
                is FirProperty,
                is FirPropertyAccessor,
                is FirField,
                is FirTypeAlias,
                is FirConstructor -> resolvePhase < FirResolvePhase.BODY_RESOLVE
                else -> true
            }
        }
        else -> {
            check(resolvePhase == FirResolvePhase.BODY_RESOLVE) {
                "Expected body resolve phase for origin $origin but found $resolvePhase"
            }
            false
        }
    }

    fun lazyResolveFileDeclaration(
        firFile: FirFile,
        moduleFileCache: ModuleFileCache,
        toPhase: FirResolvePhase,
        scopeSession: ScopeSession,
        checkPCE: Boolean = false,
    ) {
        moduleFileCache.firFileLockProvider.runCustomResolveUnderLock(firFile, checkPCE) {
            lazyResolveFileDeclarationWithoutLock(
                firFile = firFile,
                moduleFileCache = moduleFileCache,
                toPhase = toPhase,
                scopeSession = scopeSession,
                checkPCE = checkPCE,
            )
        }
    }

    private fun lazyResolveFileDeclarationWithoutLock(
        firFile: FirFile,
        moduleFileCache: ModuleFileCache,
        toPhase: FirResolvePhase,
        scopeSession: ScopeSession,
        checkPCE: Boolean = false,
        collector: FirTowerDataContextCollector? = null,
    ) {
        if (toPhase == FirResolvePhase.RAW_FIR) return
        if (firFile.resolvePhase == FirResolvePhase.RAW_FIR) {
            firFile.transform<FirElement, Any?>(FirImportResolveTransformer(firFile.moduleData.session), null)
            firFile.ensurePhase(FirResolvePhase.IMPORTS)
        }
        if (checkPCE) checkCanceled()
        if (toPhase == FirResolvePhase.IMPORTS) return

        resolveFileAnnotationsWithoutLock(firFile, moduleFileCache, firFile.annotations, scopeSession, collector)

        if (firFile.declarations.isEmpty()) return
        val designations = firFile.declarations.map {
            FirDeclarationDesignationWithFile(path = emptyList(), declaration = it, firFile = firFile)
        }

        var currentPhase = FirResolvePhase.IMPORTS
        while (currentPhase < toPhase) {
            currentPhase = currentPhase.next
            if (currentPhase.pluginPhase) continue
            if (checkPCE) checkCanceled()

            val transformersToApply = designations.mapNotNull {
                val needToResolve = it.resolvePhaseForAllDeclarations(includeDeclarationPhase = false) < currentPhase
                needToResolve.ifTrue {
                    LazyTransformerFactory.createLazyTransformer(
                        phase = currentPhase,
                        designation = it,
                        scopeSession = scopeSession,
                        declarationPhaseDowngraded = false,
                        moduleFileCache = moduleFileCache,
                        lazyDeclarationResolver = this,
                        towerDataContextCollector = null,
                        firProviderInterceptor = null,
                        checkPCE = checkPCE,
                    )
                }
            }
            if (transformersToApply.isEmpty()) continue

            firFileBuilder.firPhaseRunner.runPhaseWithCustomResolve(currentPhase) {
                for (currentTransformer in transformersToApply) {
                    currentTransformer.transformDeclaration(firFileBuilder.firPhaseRunner)
                }
            }
        }
    }

    /**
     * Run designated resolve only designation with fully resolved path (synchronized).
     * Suitable for body resolve or/and on-air resolve.
     * @see lazyResolveDeclaration for ordinary resolve
     * @param firDeclarationToResolve target non-local declaration
     */
    fun lazyResolveDeclaration(
        firDeclarationToResolve: FirDeclaration,
        moduleFileCache: ModuleFileCache,
        scopeSession: ScopeSession,
        toPhase: FirResolvePhase,
        checkPCE: Boolean,
        declarationPhaseDowngraded: Boolean = false,
    ) {
        if (toPhase == FirResolvePhase.RAW_FIR) return
        //TODO Should be synchronised
        if (!firDeclarationToResolve.isValidForResolve()) return

        if (firDeclarationToResolve is FirFile) {
            lazyResolveFileDeclaration(
                firFile = firDeclarationToResolve,
                moduleFileCache = moduleFileCache,
                toPhase = toPhase,
                scopeSession = scopeSession,
                checkPCE = checkPCE,
            )
            return
        }

        val provider = firDeclarationToResolve.moduleData.session.firIdeProvider
        val (designation, wasInLocalDeclaration) =
            firDeclarationToResolve.getNonLocalDeclarationToResolveAndInLocal(provider, moduleFileCache, firFileBuilder)

        //TODO Should be synchronised
        if (!designation.declaration.isValidForResolve()) return

        //TODO Should be synchronised
        val resolvePhase = designation.resolvePhaseForAllDeclarations(includeDeclarationPhase = declarationPhaseDowngraded)

        val neededPhase = if (wasInLocalDeclaration) {
            if (toPhase >= FirResolvePhase.CONTRACTS) FirResolvePhase.BODY_RESOLVE else maxOf(FirResolvePhase.CONTRACTS, toPhase)
        } else toPhase

        if (resolvePhase >= neededPhase) return

        moduleFileCache.firFileLockProvider.runCustomResolveUnderLock(designation.firFile, checkPCE) {
            runLazyDesignatedResolveWithoutLock(
                designation = designation,
                moduleFileCache = moduleFileCache,
                scopeSession = scopeSession,
                toPhase = neededPhase,
                checkPCE = checkPCE,
                declarationPhaseDowngraded = declarationPhaseDowngraded,
            )
        }
    }

    private fun runLazyDesignatedResolveWithoutLock(
        designation: FirDeclarationDesignationWithFile,
        moduleFileCache: ModuleFileCache,
        scopeSession: ScopeSession,
        toPhase: FirResolvePhase,
        checkPCE: Boolean,
        declarationPhaseDowngraded: Boolean,
    ) {
        val filePhase = designation.firFile.resolvePhase
        if (filePhase == FirResolvePhase.RAW_FIR) {
            lazyResolveFileDeclarationWithoutLock(
                firFile = designation.firFile,
                moduleFileCache = moduleFileCache,
                toPhase = FirResolvePhase.IMPORTS,
                scopeSession = scopeSession,
                checkPCE = checkPCE
            )
        }
        if (toPhase == FirResolvePhase.IMPORTS) return

        val designationPhase = designation.resolvePhaseForAllDeclarations(includeDeclarationPhase = declarationPhaseDowngraded)
        var currentPhase = maxOf(designationPhase, FirResolvePhase.IMPORTS)

        while (currentPhase < toPhase) {
            currentPhase = currentPhase.next
            if (currentPhase.pluginPhase) continue
            if (checkPCE) checkCanceled()

            LazyTransformerFactory.createLazyTransformer(
                phase = currentPhase,
                designation = designation,
                scopeSession = scopeSession,
                declarationPhaseDowngraded = declarationPhaseDowngraded,
                moduleFileCache = moduleFileCache,
                lazyDeclarationResolver = this,
                towerDataContextCollector = null,
                firProviderInterceptor = null,
                checkPCE = checkPCE,
            ).transformDeclaration(firFileBuilder.firPhaseRunner)
        }
    }

    internal fun runLazyDesignatedOnAirResolveToBodyWithoutLock(
        designation: FirDeclarationDesignationWithFile,
        moduleFileCache: ModuleFileCache,
        checkPCE: Boolean,
        onAirCreatedDeclaration: Boolean,
        towerDataContextCollector: FirTowerDataContextCollector?,
    ) {
        val scopeSession = ScopeSession()
        var currentPhase = maxOf(designation.declaration.resolvePhase, FirResolvePhase.IMPORTS)

        val firProviderInterceptor = onAirCreatedDeclaration.ifTrue {
            FirProviderInterceptorForIDE.createForFirElement(
                session = designation.firFile.moduleData.session,
                firFile = designation.firFile,
                element = designation.declaration
            )
        }

        while (currentPhase < FirResolvePhase.BODY_RESOLVE) {
            currentPhase = currentPhase.next
            if (currentPhase.pluginPhase) continue
            if (checkPCE) checkCanceled()

            LazyTransformerFactory.createLazyTransformer(
                phase = currentPhase,
                designation = designation,
                scopeSession = scopeSession,
                declarationPhaseDowngraded = true,
                moduleFileCache = moduleFileCache,
                lazyDeclarationResolver = this,
                towerDataContextCollector = towerDataContextCollector,
                firProviderInterceptor = firProviderInterceptor,
                checkPCE = checkPCE,
            ).transformDeclaration(firFileBuilder.firPhaseRunner)
        }
    }
}


