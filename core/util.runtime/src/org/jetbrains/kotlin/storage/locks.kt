/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.storage

import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock

private const val CHECK_CANCELLATION_PERIOD_MS: Long = 50

interface SimpleLock {
    fun lock()

    fun unlock()

    companion object {
        fun simpleLock(checkCancelled: Runnable? = null, interruptedExceptionHandler: ((InterruptedException) -> Unit)? = null) =
            if (checkCancelled != null && interruptedExceptionHandler != null) {
                CancellableSimpleLock(checkCancelled, interruptedExceptionHandler)
            } else {
                DefaultSimpleLock()
            }
    }
}

inline fun <T> SimpleLock.guarded(crossinline computable: () -> T): T {
    lock()
    return try {
        computable()
    } finally {
        unlock()
    }
}

object EmptySimpleLock : SimpleLock {
    override fun lock() {
    }

    override fun unlock() {
    }
}

open class DefaultSimpleLock(protected val lock: Lock = ReentrantLock()) : SimpleLock {

    override fun lock() = lock.lock()

    override fun unlock() = lock.unlock()

}

class CancellableSimpleLock(
    lock: Lock,
    private val checkCancelled: Runnable,
    private val interruptedExceptionHandler: (InterruptedException) -> Unit
) : DefaultSimpleLock(lock) {
    constructor(checkCancelled: Runnable, interruptedExceptionHandler: (InterruptedException) -> Unit) : this(
        checkCancelled = checkCancelled,
        lock = ReentrantLock(),
        interruptedExceptionHandler = interruptedExceptionHandler
    )

    override fun lock() {
        try {
            while (!lock.tryLock(CHECK_CANCELLATION_PERIOD_MS, TimeUnit.MILLISECONDS)) {
                //ProgressIndicatorAndCompilationCanceledStatus.checkCanceled()
                checkCancelled.run()
            }
        } catch (e: InterruptedException) {
            interruptedExceptionHandler(e)
        }
    }

}