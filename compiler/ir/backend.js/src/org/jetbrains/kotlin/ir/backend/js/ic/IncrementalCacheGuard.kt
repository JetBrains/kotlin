/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(ExperimentalContracts::class)

package org.jetbrains.kotlin.ir.backend.js.ic

import org.jetbrains.kotlin.ir.backend.js.ic.IncrementalCacheGuard.AcquireStatus
import java.io.File
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

class IncrementalCacheGuard(cacheDir: String) {
    enum class AcquireStatus { OK, CACHE_CLEARED, INVALID_CACHE }

    private val cacheRoot = File(cacheDir)
    private val guardFile = cacheRoot.resolve("cache.guard")

    fun acquire(): AcquireStatus {
        if (guardFile.exists()) {
            cacheRoot.deleteRecursively()
            tryAcquire()
            return AcquireStatus.CACHE_CLEARED
        } else {
            tryAcquire()
            return AcquireStatus.OK
        }
    }

    fun tryAcquire() {
        cacheRoot.mkdirs()
        guardFile.createNewFile()
    }

    fun release() {
        guardFile.delete()
    }
}

inline fun <R> IncrementalCacheGuard.acquireAndRelease(block: (AcquireStatus) -> R): R {
    contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }
    val status = acquire()
    return block(status).also {
        release()
    }
}

inline fun <R> IncrementalCacheGuard?.tryAcquireAndRelease(block: () -> R): R {
    contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }
    if (this == null) return block()
    tryAcquire()
    return block().also {
        release()
    }
}
