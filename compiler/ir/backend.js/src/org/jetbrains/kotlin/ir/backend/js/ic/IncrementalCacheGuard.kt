/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.ic

import org.jetbrains.kotlin.ir.backend.js.ic.IncrementalCacheGuard.AcquireStatus
import java.io.File

class IncrementalCacheGuard(cacheDir: String, private val readonly: Boolean) {
    enum class AcquireStatus { OK, CACHE_CLEARED, INVALID_CACHE }

    private val cacheRoot = File(cacheDir)
    private val guardFile = cacheRoot.resolve("cache.guard")

    fun acquire(): AcquireStatus {
        if (guardFile.exists()) {
            if (readonly) {
                return AcquireStatus.INVALID_CACHE
            } else {
                cacheRoot.deleteRecursively()
                tryAcquire()
                return AcquireStatus.CACHE_CLEARED
            }
        } else {
            tryAcquire()
            return AcquireStatus.OK
        }
    }

    fun tryAcquire() {
        if (!readonly) {
            cacheRoot.mkdirs()
            guardFile.createNewFile()
        }
    }

    fun release() {
        if (!readonly) {
            guardFile.delete()
        }
    }
}

inline fun <R> IncrementalCacheGuard.acquireAndRelease(block: (AcquireStatus) -> R): R {
    val status = acquire()
    return block(status).also {
        release()
    }
}

inline fun <R> IncrementalCacheGuard.tryAcquireAndRelease(block: () -> R): R {
    tryAcquire()
    return block().also {
        release()
    }
}
