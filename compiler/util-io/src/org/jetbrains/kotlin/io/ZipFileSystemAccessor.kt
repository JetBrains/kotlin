/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.io

import java.io.IOException
import java.lang.ref.ReferenceQueue
import java.lang.ref.SoftReference
import java.nio.file.FileSystem
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap

interface ZipFileSystemAccessor {
    fun <T> withZipFileSystem(zipFile: Path, action: (FileSystem) -> T): T
}

object ZipFileSystemInPlaceAccessor : ZipFileSystemAccessor {
    override fun <T> withZipFileSystem(zipFile: Path, action: (FileSystem) -> T): T {
        return zipFile.withZipFileSystem(action)
    }
}

/**
 * A [ZipFileSystemAccessor] that keeps the most recently used zip file systems open.
 *
 * ### Eviction
 * Each cache entry is guarded by a [SoftReference] to a dedicated *token* object ([ZipFileSystemReferenceToken])
 * rather than to the [FileSystem] itself.
 * The token is referenced from nowhere else except the local variables of the [withZipFileSystem] calls that are
 * currently using the entry. So, the token becomes eligible for garbage collection as soon as the entry is not used,
 * and the GC clears it (i.e., evicts the entry) under memory pressure, starting from the least recently used entries.
 *
 * The [FileSystem] is held strongly by the reference object. This is what makes it possible to close the file system
 * when its token is collected: if the [FileSystem] itself was the referent, there would be nothing left to close
 * after the GC has cleared the reference. The GC puts the cleared references to the [queue] of the accessor,
 * and their file systems are closed on the next call of [withZipFileSystem] or [reset] (see [closeEvictedFileSystems]).
 *
 * Note: If the accessor becomes unreachable without calling [reset], its references become unreachable as well,
 * and they are never enqueued. Then, the file systems are not closed explicitly, and their resources (file descriptors)
 * are released only by the JDK's own cleanup of the underlying file channels. So, always call [reset] when the accessor
 * is not needed anymore.
 *
 * ### Contract
 * Neither the [FileSystem] nor any [Path] obtained from it may be used outside the `action` passed to
 * [withZipFileSystem]: the file system may be closed at any time after the `action` returns.
 * This is the same contract as for [ZipFileSystemInPlaceAccessor], which closes the file system right after the `action`.
 *
 * The accessor is thread-safe: one compilation may read klibs from several threads at once
 * (for example, the Kotlin/Native backend with `-Xbackend-threads`).
 */
class ZipFileSystemCacheableAccessor : ZipFileSystemAccessor {
    private val cache = ConcurrentHashMap<Path, ZipFileSystemReference>()

    /** The GC enqueues here the references whose tokens it has cleared, i.e., the evicted cache entries. */
    private val queue = ReferenceQueue<ZipFileSystemReferenceToken>()

    override fun <T> withZipFileSystem(zipFile: Path, action: (FileSystem) -> T): T {
        closeEvictedFileSystems()

        // A strong reference to the token of the entry. It keeps the entry from being evicted
        // (and its file system from being closed) until the `action` returns.
        var token: ZipFileSystemReferenceToken? = null

        // Fast path (a lock-free read): the entry is already cached, and its token has not been cleared yet.
        // Note: `get()` also refreshes the timestamp that the GC uses to find the least recently used soft references.
        val reference: ZipFileSystemReference = cache[zipFile]?.takeIf { cached -> cached.get()?.also { token = it } != null }
            // Slow path (locking the hash bin that holds the key): open the file system and cache it. `compute()` is atomic
            // for the given `zipFile`, so the concurrent callers share the same file system rather than open one each.
            ?: cache.compute(zipFile) { _, existing ->
                token = existing?.get()

                if (token != null) {
                    // Another thread has just cached a live entry.
                    existing
                } else {
                    // There is either no entry or an evicted one. The evicted entry can just be replaced:
                    // its reference has already been discovered by the GC while it was still reachable from the `cache`,
                    // so it will be enqueued, and its file system will be closed by `closeEvictedFileSystems()`.
                    // Note: If opening fails, the exception propagates, and nothing is cached.
                    val newToken = ZipFileSystemReferenceToken()
                    token = newToken
                    ZipFileSystemReference(zipFile, zipFile.zipFileSystem(), newToken, queue)
                }
            }!!

        // Defensive check: Should never happen, as `token` is assigned in both branches above.
        checkNotNull(token)

        try {
            return action(reference.fileSystem)
        } finally {
            // Use the token after the `action` has completed. Otherwise, the JIT could treat the local variable
            // as dead during the `action`, and the GC could clear the token (and the file system would be closed)
            // while the `action` is still running.
            // `java.lang.ref.Reference.reachabilityFence()` would be the right tool, but it is not available in JDK 1.8,
            // which this module targets.
            synchronized(checkNotNull(token)) {}
        }
    }

    /**
     * Removes all entries from the cache and closes their file systems.
     *
     * Can be called concurrently with [withZipFileSystem] and with other [reset] calls: every file system is closed
     * exactly once. However, a file system that is being used by a concurrent [withZipFileSystem] call at the moment
     * is also closed, so such a call may fail.
     */
    fun reset() {
        // The evicted entries are not in the `cache` anymore if they have been replaced. Close their file systems first.
        closeEvictedFileSystems()

        val suppressedExceptions = ArrayList<Pair<Path, IOException>>()

        for ((zipFile, reference) in cache) {
            // The entry could be concurrently removed by another `reset()` or `closeEvictedFileSystems()` call.
            // Only the thread that has actually removed the entry closes its file system, so it's closed exactly once.
            if (!cache.remove(zipFile, reference)) continue

            // The file system is closed explicitly right now, so there is no need to evict the entry anymore.
            // Note: Clearing a reference does not enqueue it. However, if the GC has already cleared and enqueued it,
            // `closeEvictedFileSystems()` closes the file system once again, which is a no-op for a closed file system.
            reference.clear()

            try {
                // The file system is held strongly, so it can be closed even if the token has already been cleared by the GC.
                reference.fileSystem.close()
            } catch (e: IOException) {
                suppressedExceptions += zipFile to e
            }
        }

        if (suppressedExceptions.isNotEmpty()) {
            val message = buildString {
                appendLine("Failed to close ${suppressedExceptions.size} ZIP file systems. See 'suppressed' exceptions for details.")
                appendLine("Affected ZIP file paths:")
                suppressedExceptions.forEachIndexed { index, (path, _) ->
                    appendLine("$index. $path")
                }
            }

            val exception = IOException(message)
            suppressedExceptions.forEach { (_, suppressedException) ->
                exception.addSuppressed(suppressedException)
            }

            throw exception
        }
    }

    /**
     * Closes the file systems of the cache entries evicted by the GC.
     *
     * Can be called concurrently: [ReferenceQueue.poll] hands out every reference to only one caller.
     */
    private fun closeEvictedFileSystems() {
        while (true) {
            @Suppress("UnsafeCastWithReturn")
            val reference = queue.poll() as ZipFileSystemReference? ?: return

            // An evicted entry must not remove its replacement, which could have been cached in the meantime.
            cache.remove(reference.zipFile, reference)

            try {
                // No one uses the file system: its token has been collected, so it is not held by any `withZipFileSystem()` call.
                reference.fileSystem.close()
            } catch (_: IOException) {
                // The failure is not related to the current call, so it must not fail it. The file system is unusable anyway.
            }
        }
    }

    /**
     * A cache entry. Holds the [fileSystem] strongly, and the token, which guards the entry from eviction, softly.
     */
    private class ZipFileSystemReference(
        val zipFile: Path,
        val fileSystem: FileSystem,
        token: ZipFileSystemReferenceToken,
        queue: ReferenceQueue<ZipFileSystemReferenceToken>,
    ) : SoftReference<ZipFileSystemReferenceToken>(token, queue)

    /**
     * The referent of a [ZipFileSystemReference]. It carries no data: only its reachability matters.
     *
     * It is strongly reachable only while some [withZipFileSystem] call is using the cache entry. Otherwise, it is only
     * softly reachable from its [ZipFileSystemReference], so the GC is free to clear it, which means evicting the entry.
     */
    private class ZipFileSystemReferenceToken
}
