/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.transformers

import org.jetbrains.kotlin.analysis.low.level.api.fir.api.LLFirDesignationToResolve
import org.jetbrains.kotlin.analysis.low.level.api.fir.file.builder.LLFirLockProvider
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.transformers.FirProviderInterceptor
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.FirTowerDataContextCollector

internal object LLFirLazyTransformerExecutor {
    fun execute(
        phase: FirResolvePhase,
        designation: LLFirDesignationToResolve,
        scopeSession: ScopeSession,
        lockProvider: LLFirLockProvider,
        towerDataContextCollector: FirTowerDataContextCollector?,
        firProviderInterceptor: FirProviderInterceptor?,
    ) {
        val lazyResolver = LLFirLazyPhaseResolverByPhase.getByPhaseIfExists(phase) ?: return
        val session = designation.firFile.moduleData.session
        lazyResolver.resolve(designation, lockProvider, session, scopeSession, towerDataContextCollector, firProviderInterceptor)
        lazyResolver.checkIsResolved(designation)
    }
}