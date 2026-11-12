/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.provisioning

import java.nio.channels.FileChannel
import java.nio.channels.FileLock
import java.nio.channels.OverlappingFileLockException
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantLock

private const val OVERLAP_RETRY_MILLIS = 20L

/**
 * An exclusive lock on [lockFile] for threads of this JVM and for other processes.
 *
 * A file lock belongs to the process, and a second lock on the same file from this JVM fails instead of waiting. So threads
 * queue on a lock inside the JVM first, and only one of them takes the file lock. That inner lock lives in a static map,
 * which is per class loader. When another copy of this class, loaded by a different class loader, holds the file lock, the
 * file lock reports an overlap, and this copy retries until it is free.
 *
 * The lock file is never deleted: another process may be waiting on it.
 *
 * @param onWait called once, from the waiting thread, when the lock is not free at the first attempt.
 */
internal class InstallationLock(
    private val lockFile: Path,
    private val onWait: () -> Unit,
    private val sleep: (millis: Long) -> Unit,
) {
    fun <T> withLock(action: () -> T): T {
        val jvmLock = jvmLocks.computeIfAbsent(lockFile.toAbsolutePath().normalize()) { ReentrantLock() }
        check(!jvmLock.isHeldByCurrentThread) { "'$lockFile' is already held by this thread: an installation cannot wait for itself" }

        var waiting = false
        val startWaiting = {
            if (!waiting) {
                waiting = true
                onWait()
            }
        }

        if (!jvmLock.tryLock()) {
            startWaiting()
            jvmLock.lockInterruptibly()
        }
        try {
            FileChannel.open(lockFile, StandardOpenOption.CREATE, StandardOpenOption.WRITE).use { channel ->
                val fileLock = lockFile(channel, startWaiting)
                try {
                    return action()
                } finally {
                    fileLock.release()
                }
            }
        } finally {
            jvmLock.unlock()
        }
    }

    private fun lockFile(channel: FileChannel, startWaiting: () -> Unit): FileLock {
        while (true) {
            try {
                channel.tryLock()?.let { return it }
                startWaiting()
                return channel.lock()
            } catch (e: OverlappingFileLockException) {
                // held by this JVM through another channel, that is, by a copy of this class from another class loader
                startWaiting()
                sleep(OVERLAP_RETRY_MILLIS)
            }
        }
    }

    private companion object {
        val jvmLocks = ConcurrentHashMap<Path, ReentrantLock>()
    }
}
