/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.transformers

import org.jetbrains.kotlin.analysis.low.level.api.fir.api.targets.LLFirResolveTarget
import org.jetbrains.kotlin.analysis.low.level.api.fir.file.builder.LLFirLockProvider
import org.jetbrains.kotlin.fir.FirElementWithResolveState
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.FirResolveContextCollector

internal object LLFirSealedClassInheritorsLazyResolver : LLFirLazyResolver(FirResolvePhase.SEALED_CLASS_INHERITORS) {
    override fun resolve(
        target: LLFirResolveTarget,
        lockProvider: LLFirLockProvider,
        session: FirSession,
        scopeSession: ScopeSession,
        towerDataContextCollector: FirResolveContextCollector?,
    ) {
        val resolver = LLFirSealedClassInheritorsDesignatedResolver(target, lockProvider)
        resolver.resolveDesignation()
    }
}


private class LLFirSealedClassInheritorsDesignatedResolver(
    target: LLFirResolveTarget,
    lockProvider: LLFirLockProvider,
) : LLFirTargetResolver(target, lockProvider, FirResolvePhase.SEALED_CLASS_INHERITORS) {
    override fun doLazyResolveUnderLock(target: FirElementWithResolveState) {
        // just update the phase
    }
}
