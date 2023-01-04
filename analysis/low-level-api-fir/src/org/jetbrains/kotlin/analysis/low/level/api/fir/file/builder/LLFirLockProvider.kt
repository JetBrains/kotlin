/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.file.builder

import com.intellij.util.containers.ContainerUtil
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.FirDesignationWithFile
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

    private val locksForImports = ContainerUtil.createConcurrentSoftMap<FirFile, ReentrantLock>()

    private val superTypesLock = ReentrantLock()
    private val statusLock = ReentrantLock()
    private val bodyResolveLock = ReentrantLock()

    inline fun <R> withGlobalLock(
        @Suppress("UNUSED_PARAMETER") key: FirFile,
        lockingIntervalMs: Long = DEFAULT_LOCKING_INTERVAL,
        action: () -> R
    ): R {
        return globalLock.lockWithPCECheck(lockingIntervalMs) { action() }
    }

    fun withLock(
        designation: FirDesignationWithFile,
        phase: FirResolvePhase,
        action: () -> Unit
    ) {
        when (phase) {
            FirResolvePhase.RAW_FIR -> error("Non-lazy phase $phase")
            FirResolvePhase.IMPORTS -> error("Non-lazy phase $phase")
            FirResolvePhase.SEALED_CLASS_INHERITORS -> error("Non-lazy phase $phase")

            FirResolvePhase.SUPER_TYPES,
            FirResolvePhase.STATUS,
            FirResolvePhase.IMPLICIT_TYPES_BODY_RESOLVE,
            FirResolvePhase.BODY_RESOLVE -> {
                withGlobalPhaseLock(designation, phase, action)
            }

            FirResolvePhase.COMPILER_REQUIRED_ANNOTATIONS,
            FirResolvePhase.COMPANION_GENERATION,
            FirResolvePhase.TYPES,
            FirResolvePhase.ARGUMENTS_OF_ANNOTATIONS,
            FirResolvePhase.CONTRACTS,
            FirResolvePhase.ANNOTATIONS_ARGUMENTS_MAPPING,
            FirResolvePhase.EXPECT_ACTUAL_MATCHING -> {
                withDeclarationPhaseLock(designation, phase) { action() }
            }
        }
    }

    private inline fun withDeclarationPhaseLock(
        designation: FirDesignationWithFile,
        phase: FirResolvePhase,
        crossinline action: () -> Unit
    ) {
        designation.target.withCriticalSection(phase) {
            action()
        }
    }

    private inline fun withGlobalPhaseLock(
        designation: FirDesignationWithFile,
        phase: FirResolvePhase,
        action: () -> Unit
    ) {
        val lock = when (phase) {
            FirResolvePhase.SUPER_TYPES -> superTypesLock
            FirResolvePhase.STATUS -> statusLock
            FirResolvePhase.IMPLICIT_TYPES_BODY_RESOLVE -> bodyResolveLock
            FirResolvePhase.BODY_RESOLVE -> bodyResolveLock
            else -> error("The phase $phase does not require the global lock")
        }
        checker.lazyResolveToPhaseInside(phase) {
            lock.lockWithPCECheck(DEFAULT_LOCKING_INTERVAL) {
                action()
                designation.target.replaceResolveState(phase.asResolveState())
            }
        }
    }

    inline fun withLocksForImportResolution(
        file: FirFile,
        action: () -> Unit
    ) {
        checker.lazyResolveToPhaseInside(FirResolvePhase.IMPORTS) {
            locksForImports.getOrPut(file) { ReentrantLock() }.lockWithPCECheck(DEFAULT_LOCKING_INTERVAL) { action() }
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