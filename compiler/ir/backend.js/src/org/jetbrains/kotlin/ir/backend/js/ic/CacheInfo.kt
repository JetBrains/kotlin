/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.ic

import java.io.File

// TODO md5 hash
data class CacheInfo(
    val path: String,
    val libPath: String,
    val moduleName: String?,
    var flatHash: ULong,
    var transHash: ULong,
    var configHash: ULong
) {
    companion object {
        fun load(path: String): CacheInfo? {
            val info = File(File(path), "info")

            if (!info.exists()) return null

            val (libPath, moduleName, flatHash, transHash, configHash) = info.readLines()

            // safe cast for the backward compatibility with the cache from the previous compiler versions
            val configHashULong = configHash.toULongOrNull(16) ?: 0UL
            return CacheInfo(path, libPath, moduleName, flatHash.toULong(16), transHash.toULong(16), configHashULong)
        }

        fun loadOrCreate(path: String, libPath: String): CacheInfo {
            return load(path) ?: CacheInfo(path, libPath, null, 0UL, 0UL, 0UL)
        }
    }
}
