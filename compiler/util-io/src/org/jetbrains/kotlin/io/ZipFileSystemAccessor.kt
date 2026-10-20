/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.io

import java.nio.file.FileSystem
import java.nio.file.Path

interface ZipFileSystemAccessor {
    fun <T> withZipFileSystem(zipFile: Path, action: (FileSystem) -> T): T
}

object ZipFileSystemInPlaceAccessor : ZipFileSystemAccessor {
    override fun <T> withZipFileSystem(zipFile: Path, action: (FileSystem) -> T): T {
        return zipFile.withZipFileSystem(action)
    }
}

/**
 * A [ZipFileSystemAccessor] that keeps up to [cacheLimit] most recently used zip file systems open.
 *
 * The accessor is thread-safe: one compilation may read klibs from several threads at once
 * (for example, the Kotlin/Native backend with `-Xbackend-threads`).
 */
class ZipFileSystemCacheableAccessor(private val cacheLimit: Int) : ZipFileSystemAccessor {
    private val lock = Any()

    @Suppress("InconsistentCommentForJavaParameter")
    private val cache = object : LinkedHashMap<Path, FileSystem>(
        /* initialCapacity = */ computeInitialCapacity(cacheLimit),
        /* loadFactor = */ LOAD_FACTOR,
        /* accessOrder = */ true,
    ) {
        override fun removeEldestEntry(eldest: Map.Entry<Path, FileSystem>?): Boolean {
            if (size > cacheLimit) {
                eldest?.value?.close()
                return true
            }
            return false
        }
    }

    override fun <T> withZipFileSystem(zipFile: Path, action: (FileSystem) -> T): T {
        val fileSystem = synchronized(lock) {
            cache.getOrPut(zipFile) { zipFile.zipFileSystem() }
        }
        return action(fileSystem)
    }

    fun reset() = synchronized(lock) {
        var lastException: Exception? = null
        for (fileSystem in cache.values) {
            try {
                fileSystem.close()
            } catch (e: Exception) {
                lastException = e
            }
        }
        cache.clear()

        lastException?.let {
            throw it
        }
    }

    companion object {
        private const val LOAD_FACTOR = 0.75f

        private fun computeInitialCapacity(cacheLimit: Int) = (1f + cacheLimit.toFloat() / LOAD_FACTOR).toInt()
    }
}
