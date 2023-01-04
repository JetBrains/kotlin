/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.transformers

import org.jetbrains.kotlin.analysis.low.level.api.fir.api.LLFirDesignationToResolve
import org.jetbrains.kotlin.analysis.low.level.api.fir.file.builder.LLFirLockProvider
import org.jetbrains.kotlin.analysis.low.level.api.fir.lazy.resolve.LLFirPhaseUpdater
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.checkPhase
import org.jetbrains.kotlin.fir.FirElementWithResolveState
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.transformers.FirProviderInterceptor
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.FirTowerDataContextCollector
import org.jetbrains.kotlin.fir.resolve.transformers.plugin.FirCompanionGenerationTransformer

internal object LLFirDesignatedGeneratedCompanionObjectResolvePhaseResolver : LLFirLazyPhaseResolver() {

    override fun resolve(
        designation: LLFirDesignationToResolve,
        lockProvider: LLFirLockProvider,
        session: FirSession,
        scopeSession: ScopeSession,
        towerDataContextCollector: FirTowerDataContextCollector?,
        firProviderInterceptor: FirProviderInterceptor?
    ) {
        val resolver = LLFirCompanionGenerationTypeResolver(designation, lockProvider, session)
        resolver.resolve()
    }

    override fun updatePhaseForDeclarationInternals(target: FirElementWithResolveState) {
        LLFirPhaseUpdater.updateDeclarationInternalsPhase(target, FirResolvePhase.COMPANION_GENERATION, updateForLocalDeclarations = false)
    }

    override fun checkIsResolved(target: FirElementWithResolveState) {
        target.checkPhase(FirResolvePhase.COMPANION_GENERATION)
    }
}

private class LLFirCompanionGenerationTypeResolver(
    designation: LLFirDesignationToResolve,
    lockProvider: LLFirLockProvider,
    private val session: FirSession,
) : LLFirAbstractMultiDesignationResolver(designation, lockProvider, FirResolvePhase.COMPANION_GENERATION) {
    private val transformer: FirCompanionGenerationTransformer = FirCompanionGenerationTransformer(session)

    override fun withFile(firFile: FirFile, action: () -> Unit) {
        action()
    }

    override fun withRegularClass(firClass: FirRegularClass, action: () -> Unit) {
        action()
    }

    override fun resolveDeclarationContent(target: FirElementWithResolveState) {
        if (target !is FirRegularClass) return
        transformer.generateCompanion(target)
    }
}