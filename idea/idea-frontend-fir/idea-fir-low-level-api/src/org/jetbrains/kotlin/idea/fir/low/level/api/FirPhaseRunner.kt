/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api

import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.idea.fir.low.level.api.util.executeWithoutPCE
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

internal class FirPhaseRunner {
    private val superTypesBodyResolveLock = ReentrantLock()
    private val statusResolveLock = ReentrantLock()
    private val implicitTypesResolveLock = ReentrantLock()

    inline fun runPhaseWithCustomResolve(phase: FirResolvePhase, crossinline resolve: () -> Unit) = when (phase) {
        FirResolvePhase.SUPER_TYPES -> superTypesBodyResolveLock.withLock {
            runPhaseWithCustomResolveWithoutLock(resolve)
        }
        FirResolvePhase.STATUS -> statusResolveLock.withLock {
            runPhaseWithCustomResolveWithoutLock(resolve)
        }
        FirResolvePhase.IMPLICIT_TYPES_BODY_RESOLVE -> implicitTypesResolveLock.withLock {
            runPhaseWithCustomResolveWithoutLock(resolve)
        }
        else -> {
            runPhaseWithCustomResolveWithoutLock(resolve)
        }
    }

    private inline fun runPhaseWithCustomResolveWithoutLock(crossinline resolve: () -> Unit) {
        executeWithoutPCE {
            resolve()
        }
    }
}