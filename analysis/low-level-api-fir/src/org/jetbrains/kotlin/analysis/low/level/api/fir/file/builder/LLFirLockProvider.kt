/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.file.builder

import com.intellij.util.containers.ContainerUtil
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.FirDesignationWithFile
import org.jetbrains.kotlin.analysis.low.level.api.fir.lazy.resolve.LLFirLazyResolveContractChecker
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.lockWithPCECheck
import org.jetbrains.kotlin.fir.FirElementWithResolvePhase
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import java.util.concurrent.locks.ReentrantLock

/**
 * Keyed locks provider.
 */
internal class LLFirLockProvider(private val checker: LLFirLazyResolveContractChecker) {
    private val globalLock = ReentrantLock()

    private val locksForImports = ContainerUtil.createConcurrentSoftMap<FirFile, ReentrantLock>()
    private val locks = ContainerUtil.createConcurrentSoftMap<FirElementWithResolvePhase, ReentrantLock>()

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

    inline fun withLock(
        designation: FirDesignationWithFile,
        phase: FirResolvePhase,
        action: () -> Unit
    ) {
        val lock = when (phase) {
            FirResolvePhase.RAW_FIR -> error("Non-lazy phase $phase")
            FirResolvePhase.IMPORTS -> error("Non-lazy phase $phase")
            FirResolvePhase.SEALED_CLASS_INHERITORS -> error("Non-lazy phase $phase")
            FirResolvePhase.SUPER_TYPES -> superTypesLock
            FirResolvePhase.STATUS -> statusLock
            FirResolvePhase.IMPLICIT_TYPES_BODY_RESOLVE -> bodyResolveLock
            FirResolvePhase.BODY_RESOLVE -> bodyResolveLock
            FirResolvePhase.COMPILER_REQUIRED_ANNOTATIONS,
            FirResolvePhase.COMPANION_GENERATION,
            FirResolvePhase.TYPES,
            FirResolvePhase.ARGUMENTS_OF_ANNOTATIONS,
            FirResolvePhase.CONTRACTS,
            FirResolvePhase.ANNOTATIONS_ARGUMENTS_MAPPING,
            FirResolvePhase.EXPECT_ACTUAL_MATCHING -> {
                val lockOn = designation.firstNonFileDeclaration
                locks.getOrPut(lockOn) { ReentrantLock() }
            }
        }
        checker.lazyResolveToPhaseInside(phase) {
            lock.lockWithPCECheck(DEFAULT_LOCKING_INTERVAL) { action() }
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
}

private const val DEFAULT_LOCKING_INTERVAL = 50L