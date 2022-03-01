/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.lazy.resolve

import org.jetbrains.kotlin.analysis.low.level.api.fir.api.FirDeclarationDesignationWithFile
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.collectDesignationWithFile
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.tryCollectDesignationWithFile
import org.jetbrains.kotlin.analysis.low.level.api.fir.element.builder.getNonLocalContainingOrThisDeclaration
import org.jetbrains.kotlin.analysis.low.level.api.fir.file.builder.FirFileBuilder
import org.jetbrains.kotlin.analysis.low.level.api.fir.file.builder.ModuleFileCache
import org.jetbrains.kotlin.analysis.low.level.api.fir.file.builder.runCustomResolveUnderLock
import org.jetbrains.kotlin.analysis.low.level.api.fir.transformers.LLFirFileAnnotationsResolveTransformer
import org.jetbrains.kotlin.analysis.low.level.api.fir.transformers.LLFirFirProviderInterceptor
import org.jetbrains.kotlin.analysis.low.level.api.fir.transformers.LazyTransformerFactory
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.checkCanceled
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.findSourceNonLocalFirDeclaration
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.synthetic.FirSyntheticProperty
import org.jetbrains.kotlin.fir.declarations.synthetic.FirSyntheticPropertyAccessor
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.providers.firProvider
import org.jetbrains.kotlin.fir.resolve.transformers.FirImportResolveTransformer
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.FirTowerDataContextCollector
import org.jetbrains.kotlin.psi.KtClassBody
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtEnumEntry

internal class FirLazyDeclarationResolver(private val firFileBuilder: FirFileBuilder) {
    /**
     * Fully resolve file annotations (synchronized)
     * @see resolveFileAnnotationsWithoutLock not synchronized
     */
    fun resolveFileAnnotations(
        firFile: FirFile,
        annotations: List<FirAnnotation>,
        moduleFileCache: ModuleFileCache,
        scopeSession: ScopeSession,
        checkPCE: Boolean,
        collector: FirTowerDataContextCollector? = null,
    ) {
        if (firFile.resolvePhase >= FirResolvePhase.IMPORTS && annotations.all { it.resolved }) return
        moduleFileCache.firFileLockProvider.runCustomResolveUnderLock(firFile, checkPCE) {
            resolveFileAnnotationsWithoutLock(
                firFile = firFile,
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
        annotations: List<FirAnnotation>,
        scopeSession: ScopeSession,
        collector: FirTowerDataContextCollector? = null,
    ) {
        if (firFile.resolvePhase < FirResolvePhase.IMPORTS) {
            resolveFileToImportsWithoutLock(firFile, false)
        }

        if (!annotations.all { it.resolved }) {
            LLFirFileAnnotationsResolveTransformer(
                firFile = firFile,
                annotations = annotations,
                session = firFile.moduleData.session,
                scopeSession = scopeSession,
                firTowerDataContextCollector = collector,
            ).transformDeclaration(firFileBuilder.firPhaseRunner)
        }
    }

    private fun FirDeclaration.isValidForResolve(): Boolean = when (origin) {
        is FirDeclarationOrigin.Source,
        is FirDeclarationOrigin.ImportedFromObject,
        is FirDeclarationOrigin.Delegated,
        is FirDeclarationOrigin.Synthetic,
        is FirDeclarationOrigin.SubstitutionOverride,
        is FirDeclarationOrigin.SamConstructor,
        is FirDeclarationOrigin.IntersectionOverride -> {
            when (this) {
                is FirFile -> true
                is FirSyntheticProperty -> false
                is FirSyntheticPropertyAccessor -> false
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
        collector: FirTowerDataContextCollector? = null,
        checkPCE: Boolean = false,
    ) {
        if (toPhase == FirResolvePhase.RAW_FIR) return
        resolveFileToImports(firFile, moduleFileCache, checkPCE)
        if (toPhase == FirResolvePhase.IMPORTS) return
        if (firFile.resolvePhase >= toPhase) return
        moduleFileCache.firFileLockProvider.runCustomResolveUnderLock(firFile, checkPCE) {
            ResolveTreeBuilder.resolveEnsure(firFile, toPhase) {
                lazyResolveFileDeclarationWithoutLock(
                    firFile = firFile,
                    moduleFileCache = moduleFileCache,
                    toPhase = toPhase,
                    collector = collector,
                    scopeSession = scopeSession,
                    checkPCE = checkPCE,
                )
            }
        }
    }

    private fun resolveFileToImports(firFile: FirFile, moduleFileCache: ModuleFileCache, checkPCE: Boolean) {
        if (firFile.resolvePhase >= FirResolvePhase.IMPORTS) return
        moduleFileCache.firFileLockProvider.runCustomResolveUnderLock(firFile, checkPCE) {
            resolveFileToImportsWithoutLock(firFile, checkPCE)
        }
    }

    private fun resolveFileToImportsWithoutLock(firFile: FirFile, checkPCE: Boolean) {
        if (firFile.resolvePhase >= FirResolvePhase.IMPORTS) return
        if (checkPCE) checkCanceled()
        firFile.transform<FirElement, Any?>(FirImportResolveTransformer(firFile.moduleData.session), null)
        firFile.replaceResolvePhase(FirResolvePhase.IMPORTS)
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
        resolveFileToImportsWithoutLock(firFile, checkPCE)
        if (toPhase == FirResolvePhase.IMPORTS) return
        if (checkPCE) checkCanceled()

        resolveFileAnnotationsWithoutLock(firFile, firFile.annotations, scopeSession, collector)

        val validForResolveDeclarations = firFile.declarations
            .filter { it.isValidForResolve() && it.resolvePhase < toPhase }

        if (validForResolveDeclarations.isEmpty()) return
        val designations = validForResolveDeclarations.map {
            FirDeclarationDesignationWithFile(path = emptyList(), declaration = it, firFile = firFile)
        }

        var currentPhase = FirResolvePhase.IMPORTS
        while (currentPhase < toPhase) {
            currentPhase = currentPhase.next
            if (skipPhaseInLazyResolve(currentPhase)) continue
            if (checkPCE) checkCanceled()

            val transformersToApply = designations.mapNotNull {
                val needToResolve = it.declaration.resolvePhase < currentPhase
                if (needToResolve) {
                    LazyTransformerFactory.createLazyTransformer(
                        phase = currentPhase,
                        designation = it,
                        scopeSession = scopeSession,
                        moduleFileCache = moduleFileCache,
                        lazyDeclarationResolver = this,
                        towerDataContextCollector = collector,
                        firProviderInterceptor = null,
                        checkPCE = checkPCE,
                    )
                } else null
            }
            if (transformersToApply.isEmpty()) continue

            firFileBuilder.firPhaseRunner.runPhaseWithCustomResolve(currentPhase) {
                for (currentTransformer in transformersToApply) {
                    currentTransformer.transformDeclaration(firFileBuilder.firPhaseRunner)
                }
            }
            firFile.replaceResolvePhase(currentPhase)
        }
    }

    private fun fastTrackForImportsPhase(
        firDeclarationToResolve: FirDeclaration,
        moduleFileCache: ModuleFileCache,
        checkPCE: Boolean,
    ): Boolean {
        val provider = firDeclarationToResolve.moduleData.session.firProvider
        val firFile = when (firDeclarationToResolve) {
            is FirFile -> firDeclarationToResolve
            is FirCallableDeclaration -> provider.getFirCallableContainerFile(firDeclarationToResolve.symbol)
            is FirClassLikeDeclaration -> provider.getFirClassifierContainerFile(firDeclarationToResolve.symbol)
            else -> null
        } ?: return false
        resolveFileToImports(firFile, moduleFileCache, checkPCE)
        return true
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
    ) {
        if (toPhase == FirResolvePhase.RAW_FIR) return
        if (toPhase == FirResolvePhase.IMPORTS) {
            if (fastTrackForImportsPhase(firDeclarationToResolve, moduleFileCache, checkPCE)) {
                return
            }
        }
        when (firDeclarationToResolve) {
            is FirSyntheticPropertyAccessor -> {
                lazyResolveDeclaration(
                    firDeclarationToResolve.delegate,
                    moduleFileCache,
                    scopeSession,
                    toPhase,
                    checkPCE,
                )
                return
            }

            is FirBackingField -> {
                lazyResolveDeclaration(
                    firDeclarationToResolve.propertySymbol.fir,
                    moduleFileCache,
                    scopeSession,
                    toPhase,
                    checkPCE,
                )
            }

            is FirFile -> {
                lazyResolveFileDeclaration(
                    firFile = firDeclarationToResolve,
                    moduleFileCache = moduleFileCache,
                    toPhase = toPhase,
                    scopeSession = scopeSession,
                    checkPCE = checkPCE,
                )
                return
            }
            else -> {}
        }

        if (!firDeclarationToResolve.isValidForResolve()) return
        if (firDeclarationToResolve.resolvePhase >= toPhase) return


        val requestedDeclarationDesignation = firDeclarationToResolve.tryCollectDesignationWithFile()

        val designation: FirDeclarationDesignationWithFile
        val neededPhase: FirResolvePhase

        if (requestedDeclarationDesignation != null) {
            designation = requestedDeclarationDesignation
            neededPhase = toPhase
        } else {
            val possiblyLocalDeclaration = firDeclarationToResolve.getKtDeclarationForFirElement()
            val nonLocalDeclaration = possiblyLocalDeclaration.getNonLocalContainingOrThisDeclaration()
                ?: error("Container for local declaration cannot be null")
            val isLocalDeclarationResolveRequested =
                possiblyLocalDeclaration != nonLocalDeclaration

            val declarationToResolve: FirDeclaration

            if (isLocalDeclarationResolveRequested) {
                val enumEntry = possiblyLocalDeclaration.getContainingEnumEntryAsMemberOfEnumEntry() ?: return

                declarationToResolve = enumEntry.findSourceNonLocalFirDeclaration(
                    firFileBuilder,
                    firDeclarationToResolve.moduleData.session.firProvider.symbolProvider,
                    moduleFileCache
                )
                neededPhase = FirResolvePhase.BODY_RESOLVE
            } else {
                declarationToResolve = nonLocalDeclaration.findSourceNonLocalFirDeclaration(
                    firFileBuilder,
                    firDeclarationToResolve.moduleData.session.firProvider.symbolProvider,
                    moduleFileCache
                )
                neededPhase = toPhase
            }

            if (declarationToResolve.resolvePhase >= neededPhase) return
            if (!declarationToResolve.isValidForResolve()) return

            designation = declarationToResolve.collectDesignationWithFile()
        }

        if (designation.declaration.resolvePhase >= neededPhase) return

        if (neededPhase == FirResolvePhase.IMPORTS) {
            resolveFileToImports(designation.firFile, moduleFileCache, checkPCE)
            return
        }

        moduleFileCache.firFileLockProvider.runCustomResolveUnderLock(designation.firFile, checkPCE) {
            ResolveTreeBuilder.resolveEnsure(designation.declaration, neededPhase) {
                runLazyDesignatedResolveWithoutLock(
                    designation = designation,
                    moduleFileCache = moduleFileCache,
                    scopeSession = scopeSession,
                    toPhase = neededPhase,
                    checkPCE = checkPCE,
                )
                designation.declaration
            }
        }
    }


    private fun runLazyDesignatedResolveWithoutLock(
        designation: FirDeclarationDesignationWithFile,
        moduleFileCache: ModuleFileCache,
        scopeSession: ScopeSession,
        toPhase: FirResolvePhase,
        checkPCE: Boolean,
    ) {
        if (toPhase == FirResolvePhase.RAW_FIR) return
        resolveFileToImportsWithoutLock(designation.firFile, checkPCE)
        if (toPhase == FirResolvePhase.IMPORTS) return

        val declarationResolvePhase = designation.declaration.resolvePhase
        if (declarationResolvePhase >= toPhase) return

        var currentPhase = maxOf(declarationResolvePhase, FirResolvePhase.IMPORTS)

        while (currentPhase < toPhase) {
            currentPhase = currentPhase.next
            if (skipPhaseInLazyResolve(currentPhase)) continue
            if (checkPCE) checkCanceled()

            LazyTransformerFactory.createLazyTransformer(
                phase = currentPhase,
                designation = designation,
                scopeSession = scopeSession,
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
        resolveFileToImportsWithoutLock(designation.firFile, checkPCE)
        var currentPhase = maxOf(designation.declaration.resolvePhase, FirResolvePhase.IMPORTS)

        val scopeSession = ScopeSession()

        val firProviderInterceptor = if (onAirCreatedDeclaration) {
            LLFirFirProviderInterceptor.createForFirElement(
                session = designation.firFile.moduleData.session,
                firFile = designation.firFile,
                element = designation.declaration
            )
        } else null

        while (currentPhase < FirResolvePhase.BODY_RESOLVE) {
            currentPhase = currentPhase.next
            if (skipPhaseInLazyResolve(currentPhase)) continue
            if (checkPCE) checkCanceled()

            LazyTransformerFactory.createLazyTransformer(
                phase = currentPhase,
                designation = designation,
                scopeSession = scopeSession,
                moduleFileCache = moduleFileCache,
                lazyDeclarationResolver = this,
                towerDataContextCollector = towerDataContextCollector,
                firProviderInterceptor = firProviderInterceptor,
                checkPCE = checkPCE,
            ).transformDeclaration(firFileBuilder.firPhaseRunner)
        }
    }

    private fun skipPhaseInLazyResolve(currentPhase: FirResolvePhase): Boolean {
        return currentPhase == FirResolvePhase.COMPILER_REQUIRED_ANNOTATIONS
    }
}

private fun KtDeclaration.getContainingEnumEntryAsMemberOfEnumEntry(): KtEnumEntry? {
    val body = parent as? KtClassBody ?: return null
    return body.parent as? KtEnumEntry
}

