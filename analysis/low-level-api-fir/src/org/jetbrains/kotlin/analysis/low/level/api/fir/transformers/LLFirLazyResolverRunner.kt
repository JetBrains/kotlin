/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.transformers

import org.jetbrains.kotlin.analysis.low.level.api.fir.api.targets.LLFirResolveTarget
import org.jetbrains.kotlin.analysis.low.level.api.fir.file.builder.LLFirLockProvider
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.FirTowerDataContextCollector

internal object LLFirLazyResolverRunner {
    fun runLazyResolverByPhase(
        phase: FirResolvePhase,
        target: LLFirResolveTarget,
        scopeSession: ScopeSession,
        lockProvider: LLFirLockProvider,
        towerDataContextCollector: FirTowerDataContextCollector?,
    ) {
        val lazyResolver = LLFirLazyPhaseResolverByPhase.getByPhase(phase)
        val firFile = target.firFile
        val session = firFile.moduleData.session
        lockProvider.withGlobalLock(firFile) {
            lockProvider.withGlobalPhaseLock(phase) {
                lazyResolver.resolve(target, lockProvider, session, scopeSession, towerDataContextCollector)
            }
        }

        lazyResolver.checkIsResolved(target)
    }
}