/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api.file.builder

import com.google.common.collect.MapMaker
import org.jetbrains.kotlin.idea.fir.low.level.api.annotations.PrivateForInline
import org.jetbrains.kotlin.idea.fir.low.level.api.api.DeclarationLockType
import org.jetbrains.kotlin.idea.fir.low.level.api.util.lockWithPCECheck
import java.util.concurrent.ConcurrentMap
import java.util.concurrent.locks.ReadWriteLock
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.withLock

internal class LockProvider<KEY> {
    private val locks: ConcurrentMap<KEY, ReadWriteLock> = MapMaker().weakKeys().makeMap()

    @OptIn(PrivateForInline::class)
    private val deadLockGuard = DeadLockGuard()

    @Suppress("NOTHING_TO_INLINE")
    private inline fun getLockFor(key: KEY) = locks.getOrPut(key) { ReentrantReadWriteLock() }

    @OptIn(PrivateForInline::class)
    inline fun <R> withReadLock(key: KEY, action: () -> R): R {
        val readLock = getLockFor(key).readLock()
        return deadLockGuard.guardReadLock { readLock.withLock { action() } }
    }

    @OptIn(PrivateForInline::class)
    inline fun <R> withWriteLock(key: KEY, action: () -> R): R {
        val writeLock = getLockFor(key).writeLock()
        return deadLockGuard.guardWriteLock { writeLock.withLock { action() } }
    }

    @OptIn(PrivateForInline::class)
    inline fun <R> withWriteLockPCECheck(key: KEY, lockingIntervalMs: Long, action: () -> R): R {
        val writeLock = getLockFor(key).writeLock()
        return deadLockGuard.guardWriteLock { writeLock.lockWithPCECheck(lockingIntervalMs, action) }
    }

    inline fun <R> withLock(declaration: KEY, declarationLockType: DeclarationLockType, action: () -> R): R = when (declarationLockType) {
        DeclarationLockType.READ_LOCK -> withReadLock(declaration, action)
        DeclarationLockType.WRITE_LOCK -> withWriteLock(declaration, action)
    }
}

@PrivateForInline
internal class DeadLockGuard {
    private val readLocksCount = ThreadLocal.withInitial { 0 }

    inline fun <R> guardReadLock(action: () -> R): R {
        readLocksCount.set(readLocksCount.get() + 1)
        return try {
            action()
        } finally {
            readLocksCount.set(readLocksCount.get() - 1)
        }
    }

    inline fun <R> guardWriteLock(action: () -> R): R {

        if (readLocksCount.get() > 0) {
            throw ReadWriteDeadLockException()
        }
        return action()
    }
}

class ReadWriteDeadLockException : IllegalStateException("Acquiring write lock when read lock hold")
