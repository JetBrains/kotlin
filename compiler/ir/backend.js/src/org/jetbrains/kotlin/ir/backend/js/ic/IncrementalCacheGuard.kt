/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.ic

import java.io.File

class IncrementalCacheGuard(cacheDir: String) {
    enum class AcquireStatus { OK, CACHE_CLEARED }

    private val cacheRoot = File(cacheDir)
    private val guardFile = cacheRoot.resolve("cache.guard")

    fun acquire(): AcquireStatus {
        val cacheCleared = guardFile.exists()
        if (cacheCleared) {
            cacheRoot.deleteRecursively()
        }
        tryAcquire()
        return if (cacheCleared) AcquireStatus.CACHE_CLEARED else AcquireStatus.OK
    }

    fun tryAcquire() {
        cacheRoot.mkdirs()
        guardFile.createNewFile()
    }

    fun release() {
        guardFile.delete()
    }
}
