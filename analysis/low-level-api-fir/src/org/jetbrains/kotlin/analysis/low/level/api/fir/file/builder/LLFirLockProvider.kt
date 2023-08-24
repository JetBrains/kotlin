/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.file.builder

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.registry.Registry
import org.jetbrains.kotlin.analysis.low.level.api.fir.lazy.resolve.LLFirLazyResolveContractChecker
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.llFirSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.checkCanceled
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.lockWithPCECheck
import org.jetbrains.kotlin.fir.FirElementWithResolveState
import org.jetbrains.kotlin.fir.declarations.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater
import java.util.concurrent.locks.ReentrantLock
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

/**
 * Keyed locks provider.
 */
internal class LLFirLockProvider(private val checker: LLFirLazyResolveContractChecker) {
    private val globalLock = ReentrantLock()

    private val implicitTypesLock = ReentrantLock()

    inline fun <R> withGlobalLock(
        lockingIntervalMs: Long = DEFAULT_LOCKING_INTERVAL,
        action: () -> R,
    ): R {
        if (!globalLockEnabled) return action()

        return globalLock.lockWithPCECheck(lockingIntervalMs, action)
    }

    fun withGlobalPhaseLock(
        phase: FirResolvePhase,
        action: () -> Unit,
    ) {
        val lock = when (phase) {
            FirResolvePhase.IMPLICIT_TYPES_BODY_RESOLVE -> implicitTypesLock
            else -> null
        }

        if (lock == null) {
            action()
        } else {
            lock.lockWithPCECheck(DEFAULT_LOCKING_INTERVAL, action)
        }
    }

    /**
     * Locks an a [FirElementWithResolveState] to resolve from `phase - 1` to [phase] and
     * then updates the [FirElementWithResolveState.resolveState] to a [phase].
     * Does nothing if [target] already has at least [phase] phase.
     *
     * [action] will be executed once if [target] is not yet resolved to [phase] phase.
     *
     * @see withReadLock
     */
    inline fun withWriteLock(
        target: FirElementWithResolveState,
        phase: FirResolvePhase,
        action: () -> Unit,
    ) {
        withLock(target, phase, updatePhase = true, action)
    }

    /**
     * Locks an a [FirElementWithResolveState] to read something required for [phase].
     * Does nothing if [target] already has at least [phase] phase.
     *
     * [action] will be executed once if [target] is not yet resolved to [phase] phase.
     *
     * @see withWriteLock
     */
    inline fun withReadLock(
        target: FirElementWithResolveState,
        phase: FirResolvePhase,
        action: () -> Unit,
    ) {
        withLock(target, phase, updatePhase = false, action)
    }

    private inline fun withLock(
        target: FirElementWithResolveState,
        phase: FirResolvePhase,
        updatePhase: Boolean,
        action: () -> Unit,
    ) {
        checker.lazyResolveToPhaseInside(phase) {
            target.withCriticalSection(toPhase = phase, updatePhase = updatePhase, action = action)
        }
    }

    inline fun withJumpingLock(
        target: FirElementWithResolveState,
        phase: FirResolvePhase,
        action: () -> Unit,
    ) {
        checker.lazyResolveToPhaseInside(phase, isJumpingPhase = true) {
            target.withCriticalSection(toPhase = phase, updatePhase = true, action = action)
        }
    }

    /**
     * Locks an a [FirElementWithResolveState] to resolve from `toPhase - 1` to [toPhase] and
     * then updates the [FirElementWithResolveState.resolveState] to a [toPhase] if [updatePhase] is **true**.
     *
     * [updatePhase] == false means that we want to read some data under a lock.
     *
     * If [FirElementWithResolveState] is already at least at [toPhase], does nothing.
     *
     * Otherwise:
     *  - Marks [FirElementWithResolveState] as in a process of resovle
     *  - performs the resolve by calling [action]
     *  - updates the resolve phase to [toPhase] if [updatePhase] is **true**.
     *  - notifies other threads waiting on the same lock that the declaration is already resolved by this thread, so other threads can continue its execution.
     *
     *
     *  Contention handling:
     *  - on lock acquisition, no real lock or barrier is created. Instead, the [FirElementWithResolveState.resolveState] is updated to indicate that the declaration is being resolved now.
     *  - If some other thread tries to resolve current [FirElementWithResolveState], it changes `resolveState` and puts the barrier there. Then it awaits on it until the initial thread which hold the lock finishes its job.
     *  - This way, no barrier is used in a case when no contention arise.
     */
    private inline fun FirElementWithResolveState.withCriticalSection(
        toPhase: FirResolvePhase,
        updatePhase: Boolean,
        action: () -> Unit,
    ) {
        while (true) {
            checkCanceled()

            @OptIn(ResolveStateAccess::class)
            val stateSnapshot = resolveState
            if (stateSnapshot.resolvePhase >= toPhase) {
                // already resolved by some other thread
                return
            }

            when (stateSnapshot) {
                is FirInProcessOfResolvingToPhaseStateWithoutBarrier -> {
                    // some thread is resolving the phase, so we wait until it finishes
                    trySettingBarrier(toPhase, stateSnapshot)
                    continue
                }

                is FirInProcessOfResolvingToPhaseStateWithBarrier -> {
                    // some thread is waiting on a barrier as the declaration is being resolved, so we try too
                    waitOnBarrier(stateSnapshot)
                    continue
                }

                is FirResolvedToPhaseState -> {
                    if (!tryLock(toPhase, stateSnapshot)) continue

                    var exceptionOccurred = false
                    try {
                        action()
                    } catch (e: Throwable) {
                        exceptionOccurred = true
                        throw e
                    } finally {
                        val newPhase = if (updatePhase && !exceptionOccurred) toPhase else stateSnapshot.resolvePhase
                        unlock(toPhase = newPhase)
                    }

                    return
                }
            }
        }
    }

    private fun waitOnBarrier(
        stateSnapshot: FirInProcessOfResolvingToPhaseStateWithBarrier,
    ): Boolean {
        return stateSnapshot.barrier.await(DEFAULT_LOCKING_INTERVAL, TimeUnit.MILLISECONDS)
    }

    private fun FirElementWithResolveState.trySettingBarrier(
        toPhase: FirResolvePhase,
        stateSnapshot: FirResolveState,
    ) {
        val latch = CountDownLatch(1)
        val newState = FirInProcessOfResolvingToPhaseStateWithBarrier(toPhase, latch)
        resolveStateFieldUpdater.compareAndSet(this, stateSnapshot, newState)
    }

    private fun FirElementWithResolveState.tryLock(
        toPhase: FirResolvePhase,
        stateSnapshot: FirResolveState,
    ): Boolean {
        val newState = FirInProcessOfResolvingToPhaseStateWithoutBarrier(toPhase)
        return resolveStateFieldUpdater.compareAndSet(this, stateSnapshot, newState)
    }

    private fun FirElementWithResolveState.unlock(toPhase: FirResolvePhase) {
        when (val stateSnapshotAfter = resolveStateFieldUpdater.getAndSet(this, FirResolvedToPhaseState(toPhase))) {
            is FirInProcessOfResolvingToPhaseStateWithoutBarrier -> {}
            is FirInProcessOfResolvingToPhaseStateWithBarrier -> {
                stateSnapshotAfter.barrier.countDown()
            }
            is FirResolvedToPhaseState -> {
                error("phase is unexpectedly unlocked $stateSnapshotAfter")
            }
        }
    }
}

private val resolveStateFieldUpdater = AtomicReferenceFieldUpdater.newUpdater(
    FirElementWithResolveState::class.java,
    FirResolveState::class.java,
    "resolveState"
)

private val globalLockEnabled: Boolean by lazy(LazyThreadSafetyMode.PUBLICATION) {
    Registry.`is`("kotlin.parallel.resolve.under.global.lock", false)
}

private const val DEFAULT_LOCKING_INTERVAL = 50L