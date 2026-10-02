/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.io

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.io.path.createFile
import kotlin.io.path.exists
import kotlin.io.path.isRegularFile
import kotlin.time.Duration.Companion.milliseconds

class FileLockUtilsTest {
    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `lock provides mutual exclusion between threads`() {
        val lockFile = tempDir.resolve("test.lock")
        val activeHolders = AtomicInteger(0)
        val maxActiveHolders = AtomicInteger(0)

        runConcurrently {
            withExclusiveFileLock(lockFile, DEFAULT_RETRY_TIMEOUT) {
                val active = activeHolders.incrementAndGet()
                maxActiveHolders.accumulateAndGet(active, ::maxOf)
                Thread.sleep(20L)
                activeHolders.decrementAndGet()
            }
        }

        assertEquals(1, maxActiveHolders.get())
        assertEquals(0, activeHolders.get())
    }

    @Test
    fun `only the first lock holder produces the artifact`() {
        val lockFile = tempDir.resolve("artifact.lock")
        val artifact = tempDir.resolve("artifact")
        val producers = AtomicInteger(0)

        runConcurrently {
            withExclusiveFileLock(lockFile, DEFAULT_RETRY_TIMEOUT) {
                if (!artifact.exists()) {
                    Thread.sleep(20L)
                    artifact.createFile()
                    producers.incrementAndGet()
                }
            }
        }

        assertEquals(1, producers.get())
        assertTrue(artifact.isRegularFile())
    }

    @Test
    fun `result of action is returned`() {
        assertEquals(
            42,
            withExclusiveFileLock(tempDir.resolve("test.lock"), DEFAULT_RETRY_TIMEOUT) { 42 }
        )
    }

    @Test
    fun `lock is released and lock file is kept if action throws`() {
        val lockFile = tempDir.resolve("test.lock")

        assertThrows<IllegalStateException> {
            withExclusiveFileLock(lockFile, DEFAULT_RETRY_TIMEOUT) { error("failure") }
        }
        assertTrue(lockFile.isRegularFile())

        // The lock must be available again, including from another thread.
        val executor = Executors.newSingleThreadExecutor()
        try {
            assertEquals(
                "ok",
                executor.submit<String> { withExclusiveFileLock(lockFile, DEFAULT_RETRY_TIMEOUT) { "ok" } }.get(1, TimeUnit.SECONDS)
            )
        } finally {
            executor.shutdownNow()
        }
        assertTrue(lockFile.isRegularFile())
    }

    @Test
    fun `missing parent directories of lock file are created`() {
        val lockFile = tempDir.resolve("a/b/c/test.lock")

        withExclusiveFileLock(lockFile, DEFAULT_RETRY_TIMEOUT) {}

        assertTrue(lockFile.isRegularFile())
    }

    private fun runConcurrently(threads: Int = 8, action: () -> Unit) {
        val executor = Executors.newFixedThreadPool(threads)
        try {
            val startLatch = CountDownLatch(1)
            val futures = List(threads) {
                executor.submit {
                    startLatch.await()
                    action()
                }
            }
            startLatch.countDown()
            futures.forEach { it.get(1, TimeUnit.MINUTES) }
        } finally {
            executor.shutdownNow()
        }
    }

    companion object {
        private val DEFAULT_RETRY_TIMEOUT = 200.milliseconds
    }
}
