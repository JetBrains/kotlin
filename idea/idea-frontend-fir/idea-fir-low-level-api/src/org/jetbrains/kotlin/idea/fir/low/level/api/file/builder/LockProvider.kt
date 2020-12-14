/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api.file.builder

import com.google.common.collect.MapMaker
import com.intellij.openapi.diagnostic.logger
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.idea.fir.low.level.api.util.lockWithPCECheck
import java.util.concurrent.ConcurrentMap
import java.util.concurrent.locks.ReadWriteLock
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.withLock

internal class LockProvider<KEY> {
    private val locks: ConcurrentMap<KEY, ReadWriteLock> = MapMaker().weakKeys().makeMap()

    @Suppress("NOTHING_TO_INLINE")
    private inline fun getLockFor(key: KEY) = locks.getOrPut(key) { ReentrantReadWriteLock() }

    inline fun <R> withReadLock(key: KEY, action: () -> R): R {
        val readLock = getLockFor(key).readLock()
        return readLock.withLock { action() }
    }


    inline fun <R> withWriteLock(key: KEY, action: () -> R): R {
        val writeLock = getLockFor(key).writeLock()
        return writeLock.withLock { action() }
    }

    inline fun <R> withWriteLockPCECheck(key: KEY, lockingIntervalMs: Long, action: () -> R): R {
        val writeLock = getLockFor(key).writeLock()
        return writeLock.lockWithPCECheck(lockingIntervalMs, action)
    }
}