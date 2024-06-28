/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.transformers

import org.jetbrains.kotlin.analysis.low.level.api.fir.LLFirGlobalResolveComponents
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.targets.LLFirResolveTarget
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.targets.session
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase

/**
 * Runs required [LLFirLazyResolver] on [LLFirResolveTarget] based on [FirResolvePhase].
 *
 * @see runLazyResolverByPhase
 * @see LLFirLazyResolver
 * @see org.jetbrains.kotlin.analysis.low.level.api.fir.file.builder.LLFirLockProvider
 */
internal object LLFirLazyResolverRunner {
    /**
     * Runs [resolver][LLFirLazyResolver] associated with [phase] for [target].
     *
     * @see LLFirLazyPhaseResolverByPhase
     */
    fun runLazyResolverByPhase(phase: FirResolvePhase, target: LLFirResolveTarget) {
        val lazyResolver = LLFirLazyPhaseResolverByPhase.getByPhase(phase)
        LLFirGlobalResolveComponents.getInstance(target.session).lockProvider.withGlobalLock {
            lazyResolver.resolve(target)
        }
    }
}