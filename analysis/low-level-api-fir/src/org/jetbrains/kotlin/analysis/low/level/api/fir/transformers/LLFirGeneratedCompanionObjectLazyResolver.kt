/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.transformers

import org.jetbrains.kotlin.analysis.low.level.api.fir.api.targets.LLFirResolveTarget
import org.jetbrains.kotlin.analysis.low.level.api.fir.file.builder.LLFirLockProvider
import org.jetbrains.kotlin.analysis.low.level.api.fir.lazy.resolve.LLFirPhaseUpdater
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.checkPhase
import org.jetbrains.kotlin.fir.FirElementWithResolveState
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.FirTowerDataContextCollector
import org.jetbrains.kotlin.fir.resolve.transformers.plugin.FirCompanionGenerationTransformer

internal object LLFirGeneratedCompanionObjectLazyResolver : LLFirLazyResolver(FirResolvePhase.COMPANION_GENERATION) {
    override fun resolve(
        target: LLFirResolveTarget,
        lockProvider: LLFirLockProvider,
        session: FirSession,
        scopeSession: ScopeSession,
        towerDataContextCollector: FirTowerDataContextCollector?,
    ) {
        val resolver = LLFirCompanionGenerationTargetResolver(target, lockProvider, session)
        resolver.resolveDesignation()
    }

    override fun updatePhaseForDeclarationInternals(target: FirElementWithResolveState) {
        LLFirPhaseUpdater.updateDeclarationInternalsPhase(target, resolverPhase, updateForLocalDeclarations = false)
    }

    override fun checkIsResolved(target: FirElementWithResolveState) {
        target.checkPhase(resolverPhase)
    }
}

private class LLFirCompanionGenerationTargetResolver(
    target: LLFirResolveTarget,
    lockProvider: LLFirLockProvider,
    session: FirSession,
) : LLFirTargetResolver(target, lockProvider, FirResolvePhase.COMPANION_GENERATION) {
    private val transformer: FirCompanionGenerationTransformer = FirCompanionGenerationTransformer(session)

    override fun withFile(firFile: FirFile, action: () -> Unit) {
        action()
    }

    @Deprecated("Should never be called directly, only for override purposes, please use withRegularClass", level = DeprecationLevel.ERROR)
    override fun withRegularClassImpl(firClass: FirRegularClass, action: () -> Unit) {
        action()
    }

    override fun doLazyResolveUnderLock(target: FirElementWithResolveState) {
        if (target !is FirRegularClass) return
        transformer.generateAndUpdateCompanion(target)
    }
}