/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.ic

import java.io.File
import java.io.PrintWriter

fun checkCaches(
    dependencies: Collection<String>,
    cachePaths: List<String>,
    skipLib: String? = null,
): IcCacheInfo {
    val skipLibPath = File(skipLib).canonicalPath
    val allLibs = dependencies.map { File(it).canonicalPath }.toSet() - skipLibPath

    val caches = cachePaths.map { CacheInfo.load(it) ?: error("Cannot load IC cache from ${it}") }

    val missedLibs = allLibs - caches.map { it.libPath }
    if (!missedLibs.isEmpty()) {
        error("Missing caches for libraries: ${missedLibs}")
    }

    val result = mutableMapOf<String, SerializedIcData>()
    val md5 = mutableMapOf<String, ULong>()

    for (c in caches) {
        if (c.libPath !in allLibs) error("Missing library: ${c.libPath}")

        result[c.libPath] = File(c.path).readIcData()
        md5[c.libPath] = c.flatHash
    }

    return IcCacheInfo(result, md5)
}

// TODO md5 hash
data class CacheInfo(val path: String, val libPath: String, var flatHash: ULong, var transHash: ULong, var configHash: ULong) {
    fun save() {
        PrintWriter(File(File(path), "info")).use {
            it.println(libPath)
            it.println(flatHash.toString(16))
            it.println(transHash.toString(16))
            it.println(configHash.toString(16))
        }
    }

    companion object {
        fun load(path: String): CacheInfo? {
            val info = File(File(path), "info")

            if (!info.exists()) return null

            val (libPath, flatHash, transHash, configHash) = info.readLines()

            // safe cast for the backward compatibility with the cache from the previous compiler versions
            val configHashULong = configHash.toULongOrNull(16) ?: 0UL
            return CacheInfo(path, libPath, flatHash.toULong(16), transHash.toULong(16), configHashULong)
        }
    }
}


class IcCacheInfo(
    val data: Map<String, SerializedIcData>,
    val md5: Map<String, ULong>,
) {
    companion object {
        val EMPTY = IcCacheInfo(emptyMap(), emptyMap())
    }
}