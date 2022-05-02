/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.file.builder

import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.analysis.low.level.api.fir.lazy.resolve.ResolveTreeBuilder
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.lockWithPCECheck
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * Keyed locks provider.
 */
internal class LLFirLockProvider {
    //We temporarily disable multi-locks to fix deadlocks problem
    private val globalLock = ReentrantLock()

    inline fun <R> withWriteLock(@Suppress("UNUSED_PARAMETER") key: FirFile, action: () -> R): R {
        val startTime = System.currentTimeMillis()
        return globalLock.withLock { ResolveTreeBuilder.lockNode(startTime, action) }
    }


    inline fun <R> withWriteLockPCECheck(@Suppress("UNUSED_PARAMETER") key: FirFile, lockingIntervalMs: Long, action: () -> R): R {
        val startTime = System.currentTimeMillis()
        return globalLock.lockWithPCECheck(lockingIntervalMs) { ResolveTreeBuilder.lockNode(startTime, action) }
    }
}

/**
 * Runs [resolve] function (which is considered to do some resolve on [firFile]) under a lock for [firFile]
 */
internal inline fun <R> LLFirLockProvider.runCustomResolveUnderLock(
    firFile: FirFile,
    checkPCE: Boolean,
    body: () -> R
): R {
    return if (checkPCE) {
        withWriteLockPCECheck(key = firFile, lockingIntervalMs = 50L, body)
    } else {
        withWriteLock(key = firFile, action = body)
    }
}