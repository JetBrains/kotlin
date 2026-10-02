/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.io

import java.nio.channels.FileChannel
import java.nio.channels.FileLock
import java.nio.channels.OverlappingFileLockException
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import kotlin.io.path.absolute
import kotlin.io.path.createDirectories
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * Acquires an exclusive OS-level file lock on [lockFile], runs [action] under the lock, and returns its result.
 * The lock file (and its parent directories) are created if they didn't exist before.
 *
 * The lock provides mutual exclusion both between different processes and between different threads
 * of the same JVM process. A typical usage pattern is "check-then-produce": Under the lock, check
 * whether the artifact was already produced (e.g., by another thread or process that held the lock before),
 * and if not, produce it.
 *
 * The lock file is intentionally **never deleted**. If you delete the lock file after
 * releasing the lock, this race appears:
 *  1. Process A holds a lock on `cache.lock`.
 *  2. Process B has already opened that file and is blocked waiting on the lock.
 *  3. Process A releases the lock and deletes `cache.lock`.
 *  4. Process C creates a new `cache.lock` and locks that new file.
 *  5. Process B acquires the lock on the old, now-unlinked inode it opened earlier.
 *
 * Now B and C both think they own "the" cache lock, but they are locking different
 * filesystem inodes. That breaks mutual exclusion.
 */
fun <T> withExclusiveFileLock(lockFile: Path, retryTimeout: Duration, action: () -> T): T {
    val absoluteLockFile = lockFile.absolute()
    absoluteLockFile.parent.createDirectories()

    return FileChannel.open(
        absoluteLockFile,
        StandardOpenOption.CREATE,
        StandardOpenOption.READ,
        StandardOpenOption.WRITE
    ).use { channel ->
        channel.acquireLockWithRetry(retryTimeout.inWholeMilliseconds).use { action() }
    }
}

private fun FileChannel.acquireLockWithRetry(retryTimeoutInMillis: Long): FileLock {
    while (true) {
        try {
            return this.lock()
        } catch (_: OverlappingFileLockException) {
            // Another thread in the same JVM holds the lock. Just wait:
            // - if that thread dies with a crash, the whole process dies.
            // - if that thread fails with an exception, the lock is released.
            // - if that thread hangs, we might be tight on resources, so waiting is wise.
            Thread.sleep(retryTimeoutInMillis)
        }
    }
}
