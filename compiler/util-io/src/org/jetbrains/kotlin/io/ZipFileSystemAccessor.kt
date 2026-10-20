/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.io

import java.io.IOException
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
 * A [ZipFileSystemAccessor] that caches up to [cacheLimit] most recently used ZIP file systems.
 *
 * Idle file systems are closed synchronously when the limit is exceeded, without waiting for GC or a subsequent cache access.
 * File systems currently in use are never evicted. Active calls can temporarily exceed the limit, including nested calls;
 * the limit is restored as calls finish.
 *
 * Neither the [FileSystem] nor any [Path] obtained from it may be used outside the `action` passed to [withZipFileSystem].
 * Call [reset] when the accessor is no longer needed to release any remaining idle file systems.
 *
 * Failures to close evicted file systems are ignored. Failures to close file systems in [reset] are rethrown.
 *
 * The accessor is thread-safe. Actions run outside the cache lock, including concurrent actions for the same ZIP file.
 *
 * @param cacheLimit Maximum number of cached file systems. Zero disables retaining idle file systems.
 */
class ZipFileSystemCacheableAccessor(private val cacheLimit: Int) : ZipFileSystemAccessor {
    init {
        require(cacheLimit >= 0) { "Cache limit must be non-negative: $cacheLimit" }
    }

    private val lock = Any()

    @Suppress("InconsistentCommentForJavaParameter")
    private val cache = LinkedHashMap<Path, ZipFileSystemEntry>(
        /* initialCapacity = */ computeInitialCapacity(cacheLimit),
        /* loadFactor = */ LOAD_FACTOR,
        /* accessOrder = */ true,
    )

    override fun <T> withZipFileSystem(zipFile: Path, action: (FileSystem) -> T): T {
        val entry = synchronized(lock) {
            val entry = cache[zipFile] ?: run {
                // Make room before opening another archive.
                evictIdleEntries(incomingCount = 1)

                val newEntry = ZipFileSystemEntry(zipFile.zipFileSystem())
                cache[zipFile] = newEntry
                newEntry
            }
            entry.users++
            entry
        }
        try {
            return action(entry.fileSystem)
        } finally {
            synchronized(lock) {
                entry.users--
                evictIdleEntries(incomingCount = 0)
            }
        }
    }

    fun reset(): Unit = synchronized(lock) {
        val closeFailures = ArrayList<Exception>()

        try {
            for (entry in cache.values) {
                try {
                    entry.fileSystem.close()
                } catch (e: Exception) {
                    closeFailures += e
                }
            }
        } finally {
            cache.clear()
        }

        when (closeFailures.size) {
            0 -> Unit
            1 -> throw closeFailures[0]
            else -> throw IOException("Failed to close ${closeFailures.size} ZIP file systems").apply {
                closeFailures.forEach(::addSuppressed)
            }
        }
    }

    // All cache bookkeeping, opening and closing runs under lock. Only actions execute outside it.
    private fun evictIdleEntries(incomingCount: Int) {
        if (!shouldEvictIdleEntries(incomingCount)) return

        // Iterate the entries from the least recently used to the most recently used.
        val iterator = cache.entries.iterator()
        while (shouldEvictIdleEntries(incomingCount) && iterator.hasNext()) {
            val entry = iterator.next().value
            if (entry.users != 0) continue
            iterator.remove()

            try {
                entry.fileSystem.close()
            } catch (_: Exception) {
                // Ignore: the evicted file system may belong to an unrelated archive, so its failure must not fail
                // the current caller. The entry is already removed from the cache, and callers are expected to only read.
            }
        }
    }

    private fun shouldEvictIdleEntries(incomingCount: Int): Boolean = cache.isNotEmpty() && cache.size + incomingCount > cacheLimit

    private class ZipFileSystemEntry(val fileSystem: FileSystem) {
        var users = 0
    }

    companion object {
        private const val LOAD_FACTOR = 0.75f

        private fun computeInitialCapacity(cacheLimit: Int) = (1f + cacheLimit.toFloat() / LOAD_FACTOR).toInt()
    }
}
