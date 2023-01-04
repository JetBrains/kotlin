/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.file.builder

import org.jetbrains.kotlin.analysis.low.level.api.fir.lazy.resolve.LLFirLazyResolveContractChecker
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.checkCanceled
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.lockWithPCECheck
import org.jetbrains.kotlin.fir.FirElementWithResolveState
import org.jetbrains.kotlin.fir.declarations.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater
import java.util.concurrent.locks.ReentrantLock

/**
 * Keyed locks provider.
 */
internal class LLFirLockProvider(private val checker: LLFirLazyResolveContractChecker) {
    private val globalLock = ReentrantLock()

    inline fun <R> withGlobalLock(
        @Suppress("UNUSED_PARAMETER") key: FirFile,
        lockingIntervalMs: Long = DEFAULT_LOCKING_INTERVAL,
        action: () -> R
    ): R {
        return globalLock.lockWithPCECheck(lockingIntervalMs) { action() }
    }

    fun withLock(
        target: FirElementWithResolveState,
        phase: FirResolvePhase,
        action: () -> Unit
    ) {
        checker.lazyResolveToPhaseInside(phase) {
            target.withCriticalSection(phase) {
                action()
            }
        }
    }

    private fun FirElementWithResolveState.withCriticalSection(
        toPhase: FirResolvePhase,
        action: () -> Unit
    ) {
        while (true) {
            checkCanceled()
            val stateSnapshot = resolveState
            if (stateSnapshot.resolvePhase >= toPhase) return
            when (stateSnapshot) {
                is FirInProcessOfResolvingToPhaseStateWithoutLatch -> {
                    val latch = CountDownLatch(1)
                    val newState = FirInProcessOfResolvingToPhaseStateWithLatch(toPhase, latch)
                    resolveStateFieldUpdater.compareAndSet(this, stateSnapshot, newState)
                    continue
                }
                is FirInProcessOfResolvingToPhaseStateWithLatch -> {
                    if (!stateSnapshot.latch.await(DEFAULT_LOCKING_INTERVAL, TimeUnit.MILLISECONDS)) continue
                    break
                }
                is FirResolvedToPhaseState -> {
                    val newState = FirInProcessOfResolvingToPhaseStateWithoutLatch(toPhase)
                    if (!resolveStateFieldUpdater.compareAndSet(this, stateSnapshot, newState)) {
                        continue
                    }
                    try {
                        action()
                    } finally {
                        val stateSnapshotAfter = resolveState
                        when (stateSnapshotAfter) {
                            is FirInProcessOfResolvingToPhaseStateWithoutLatch -> {}
                            is FirInProcessOfResolvingToPhaseStateWithLatch -> {
                                stateSnapshotAfter.latch.countDown()
                            }
                            is FirResolvedToPhaseState -> {
                                error("phase is unexpectedly unlocked $stateSnapshot")
                            }
                        }
                        if (!resolveStateFieldUpdater.compareAndSet(this, stateSnapshotAfter, FirResolvedToPhaseState(toPhase))) {
                            error("phase was updated by other thread, expected: $stateSnapshotAfter, actual: $resolveState")
                        }
                    }
                }
            }
            break
        }
    }
}

private val resolveStateFieldUpdater = AtomicReferenceFieldUpdater.newUpdater(
    FirElementWithResolveState::class.java,
    FirResolveState::class.java,
    "resolveState"
)

private const val DEFAULT_LOCKING_INTERVAL = 50L