/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.provisioning

import java.io.IOException
import java.lang.management.ManagementFactory
import java.nio.channels.FileChannel
import java.nio.channels.OverlappingFileLockException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock

private const val FIRST_POLL_MILLIS = 20L
private const val MAX_POLL_MILLIS = 500L
private const val MAX_HOLDER_LENGTH = 200
private const val HOLDER_SUFFIX = ".holder"

/**
 * An exclusive lock on [lockFile] for threads of this JVM and for other processes.
 *
 * Other processes are kept out by a lock on the file. On Linux and macOS such a lock has a trap: closing any descriptor
 * of the file releases every lock the process holds on it, whoever took it. So a JVM must never have the lock file open
 * twice, and three things make sure of that:
 * - threads queue on a lock inside the JVM, and only the one that has it opens the file;
 * - that lock lives in a static map, which is per class loader, so copies of this class from different class loaders
 *   also take turns, on the monitor of an interned string, which is one object for the whole JVM;
 * - the holder writes its name, `pid@host`, into a second file next to the lock file, so that a waiting caller can read
 *   who holds the lock without touching the lock file.
 *
 * The lock file is never deleted: another process may be waiting on it.
 *
 * @param onWait called once, from the waiting thread, when the lock is not free at the first attempt. It gets the holder's
 * `pid@host` if that is known.
 * @param timeoutMillis how long to wait before giving up with [InstallationLockTimeoutException]; null waits without limit.
 * A copy of this class from another class loader is waited for without limit and cannot be interrupted either way.
 */
internal class InstallationLock(
    lockFile: Path,
    private val onWait: (holder: String?) -> Unit,
    private val sleep: (millis: Long) -> Unit,
    private val timeoutMillis: Long? = null,
    private val nanoTime: () -> Long = System::nanoTime,
) {
    private val lockFile = lockFile.toAbsolutePath().normalize()
    private val holderFile = this.lockFile.resolveSibling(this.lockFile.fileName.toString() + HOLDER_SUFFIX)

    fun <T> withLock(action: () -> T): T {
        val classLoaderLock = classLoaderLocks.computeIfAbsent(lockFile) { ReentrantLock() }
        check(!classLoaderLock.isHeldByCurrentThread) {
            "'$lockFile' is already held by this thread: an installation cannot wait for itself"
        }

        val deadline = timeoutMillis?.let { nanoTime() + TimeUnit.MILLISECONDS.toNanos(it) }
        var waiting = false
        val startWaiting = {
            if (!waiting) {
                waiting = true
                onWait(readHolder())
            }
        }

        if (!classLoaderLock.tryLock()) {
            startWaiting()
            if (deadline == null) {
                classLoaderLock.lockInterruptibly()
            } else if (!classLoaderLock.tryLock(deadline - nanoTime(), TimeUnit.NANOSECONDS)) {
                throw timedOut()
            }
        }
        try {
            synchronized("$JVM_MONITOR_PREFIX$lockFile".intern()) {
                // closing the channel is what releases the file lock
                FileChannel.open(lockFile, StandardOpenOption.CREATE, StandardOpenOption.WRITE).use { channel ->
                    lockFile(channel, deadline, startWaiting)
                    writeHolder()
                    return action()
                }
            }
        } finally {
            classLoaderLock.unlock()
        }
    }

    /** Polls for the file lock: a blocking request could not be given up when the timeout runs out. */
    private fun lockFile(channel: FileChannel, deadline: Long?, startWaiting: () -> Unit) {
        var pollMillis = FIRST_POLL_MILLIS
        while (true) {
            val lock = try {
                channel.tryLock()
            } catch (e: OverlappingFileLockException) {
                // some other code in this JVM has locked the file; all that can be done is to wait for it
                null
            }
            if (lock != null) return

            startWaiting()
            val remainingMillis = deadline?.let { TimeUnit.NANOSECONDS.toMillis(it - nanoTime()) }
            if (remainingMillis != null && remainingMillis <= 0) throw timedOut()
            sleep(minOf(pollMillis, remainingMillis ?: pollMillis))
            pollMillis = minOf(pollMillis * 2, MAX_POLL_MILLIS)
        }
    }

    private fun writeHolder() {
        try {
            Files.write(holderFile, processName.toByteArray(Charsets.UTF_8))
        } catch (e: IOException) {
            // The name only helps whoever waits for the lock. The lock works without it.
        }
    }

    /**
     * Who holds the lock, as far as the holder file tells. Right after the lock changes hands, it can still name the
     * previous holder.
     */
    private fun readHolder(): String? =
        try {
            String(Files.readAllBytes(holderFile), Charsets.UTF_8).lineSequence().firstOrNull()?.trim()?.take(MAX_HOLDER_LENGTH)
                ?.takeIf { it.isNotEmpty() }
        } catch (e: IOException) {
            null
        }

    private fun timedOut() = InstallationLockTimeoutException(lockFile, readHolder(), checkNotNull(timeoutMillis))

    private companion object {
        const val JVM_MONITOR_PREFIX = "org.jetbrains.kotlin.buildtools.provisioning.InstallationLock:"

        val classLoaderLocks = ConcurrentHashMap<Path, ReentrantLock>()

        /** `pid@host` on the JVMs in use. The management classes can be missing from a trimmed-down runtime. */
        val processName: String = try {
            ManagementFactory.getRuntimeMXBean().name
        } catch (e: LinkageError) {
            "an unknown process"
        }
    }
}
