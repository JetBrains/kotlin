/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.file.builder

import org.jetbrains.kotlin.analysis.api.impl.barebone.annotations.PrivateForInline
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.analysis.low.level.api.fir.lazy.resolve.ResolveTreeBuilder
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.lockWithPCECheck
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * Keyed locks provider.
 * !!! We temporary remove its correct implementation to fix deadlocks problem. Do not use this until this comment is present
 */
internal class LockProvider<KEY> {
    //We temporary disable multi-locks to fix deadlocks problem
    private val globalLock = ReentrantLock()

    @OptIn(PrivateForInline::class)
    inline fun <R> withWriteLock(@Suppress("UNUSED_PARAMETER") key: KEY, action: () -> R): R {
        val startTime = System.currentTimeMillis()
        return globalLock.withLock { ResolveTreeBuilder.lockNode(startTime, action) }
    }


    @OptIn(PrivateForInline::class)
    inline fun <R> withWriteLockPCECheck(@Suppress("UNUSED_PARAMETER") key: KEY, lockingIntervalMs: Long, action: () -> R): R {
        val startTime = System.currentTimeMillis()
        return globalLock.lockWithPCECheck(lockingIntervalMs) { ResolveTreeBuilder.lockNode(startTime, action) }
    }
}

/**
 * Runs [resolve] function (which is considered to do some resolve on [firFile]) under a lock for [firFile]
 */
internal inline fun <R> LockProvider<FirFile>.runCustomResolveUnderLock(
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