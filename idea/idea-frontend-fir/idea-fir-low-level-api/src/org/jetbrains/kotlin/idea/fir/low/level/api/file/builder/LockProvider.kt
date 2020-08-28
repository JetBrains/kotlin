/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api.file.builder

import com.google.common.collect.MapMaker
import java.util.concurrent.ConcurrentMap
import java.util.concurrent.locks.ReadWriteLock
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

internal class LockProvider<KEY, out LOCK>(private val createLock: () -> LOCK) {
    private val locks: ConcurrentMap<KEY, LOCK> = MapMaker().weakKeys().makeMap()
    fun getLockFor(key: KEY) = locks.getOrPut(key) { createLock() }
}

internal inline fun <KEY, R> LockProvider<KEY, ReadWriteLock>.withReadLock(key: KEY, action: () -> R): R {
    val readLock = getLockFor(key).readLock()
    return readLock.withLock { action() }
}

internal inline fun <KEY, R> LockProvider<KEY, ReadWriteLock>.withWriteLock(key: KEY, action: () -> R): R {
    val writeLock = getLockFor(key).writeLock()
    return writeLock.withLock { action() }
}


internal inline fun <KEY, R> LockProvider<KEY, ReentrantLock>.withLock(key: KEY, action: () -> R): R {
    val lock = getLockFor(key)
    return lock.withLock { action() }
}