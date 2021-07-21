/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api.file.builder

import com.google.common.collect.MapMaker
import com.google.common.util.concurrent.CycleDetectingLockFactory
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.idea.fir.low.level.api.annotations.PrivateForInline
import org.jetbrains.kotlin.idea.fir.low.level.api.util.lockWithPCECheck
import java.util.concurrent.ConcurrentMap
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

internal class LockProvider<KEY> {
    private val locks: ConcurrentMap<KEY, ReentrantLock> = MapMaker().weakKeys().makeMap()

    @Suppress("UnstableApiUsage")
    private val lockFactory = CycleDetectingLockFactory.newInstance(CycleDetectingLockFactory.Policies.THROW)

    @Suppress("NOTHING_TO_INLINE")
    private inline fun getLockFor(key: KEY) = locks.getOrPut(key) {
        val file = key as FirFile
        val name = "${file.packageDirective.packageFqName.asString()}.${file.name}"
        @Suppress("UnstableApiUsage")
        lockFactory.newReentrantLock(name)
    }

    @OptIn(PrivateForInline::class)
    inline fun <R> withWriteLock(key: KEY, action: () -> R): R =
        getLockFor(key).withLock(action)

    @OptIn(PrivateForInline::class)
    inline fun <R> withWriteLockPCECheck(key: KEY, lockingIntervalMs: Long, action: () -> R): R =
        getLockFor(key).lockWithPCECheck(lockingIntervalMs, action)
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
        withWriteLockPCECheck(key = firFile, lockingIntervalMs = 50L, action = body)
    } else {
        withWriteLock(key = firFile, action = body)
    }
}