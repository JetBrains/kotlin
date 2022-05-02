/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir

import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.executeWithoutPCE

internal class LLFirPhaseRunner {
    /**
     * We temporary disable multi-locks to fix deadlocks problem
     * @see org.jetbrains.kotlin.analysis.low.level.api.fir.file.builder.LLFirLockProvider
     */
    inline fun runPhaseWithCustomResolve(@Suppress("UNUSED_PARAMETER") phase: FirResolvePhase, crossinline resolve: () -> Unit) =
        runPhaseWithCustomResolveWithoutLock(resolve)

    private inline fun runPhaseWithCustomResolveWithoutLock(crossinline resolve: () -> Unit) {
        executeWithoutPCE {
            resolve()
        }
    }
}