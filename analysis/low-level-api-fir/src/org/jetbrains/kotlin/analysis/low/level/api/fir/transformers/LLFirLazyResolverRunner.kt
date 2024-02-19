/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.transformers

import org.jetbrains.kotlin.analysis.low.level.api.fir.LLFirGlobalResolveComponents
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.targets.LLFirResolveTarget
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.targets.session
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase

internal object LLFirLazyResolverRunner {
    fun runLazyResolverByPhase(
        phase: FirResolvePhase,
        target: LLFirResolveTarget,
    ) {
        val lazyResolver = LLFirLazyPhaseResolverByPhase.getByPhase(phase)
        val lockProvider = LLFirGlobalResolveComponents.getInstance(target.session).lockProvider
        lockProvider.withGlobalLock {
            lockProvider.withGlobalPhaseLock(phase) {
                lazyResolver.resolve(target)
            }
        }

        lazyResolver.checkIsResolved(target)
    }
}