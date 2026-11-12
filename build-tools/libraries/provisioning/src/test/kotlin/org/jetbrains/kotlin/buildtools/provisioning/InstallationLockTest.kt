/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.provisioning

import org.junit.jupiter.api.io.TempDir
import java.nio.channels.FileChannel
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import kotlin.concurrent.thread
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue

class InstallationLockTest {

    @TempDir
    lateinit var tempDir: Path

    private val lockFile: Path get() = tempDir.resolve("tool.lock")

    @Test
    fun `runs the action and returns its result`() {
        val waited = CountDownLatch(1)
        assertEquals("done", InstallationLock(lockFile, onWait = { waited.countDown() }, sleep = Thread::sleep).withLock { "done" })
        assertEquals(1L, waited.count, "a free lock does not report waiting")
    }

    @Test
    fun `waits while a copy of the lock from another class loader holds the file`() {
        // Another class loader has its own in-JVM lock, so the only thing in the way is the file lock of this JVM.
        // A second channel on the same file in this JVM is exactly what that copy would hold.
        val waiting = CountDownLatch(1)
        val executor = Executors.newSingleThreadExecutor()
        try {
            val result = FileChannel.open(lockFile, StandardOpenOption.CREATE, StandardOpenOption.WRITE).use { channel ->
                val held = channel.lock()
                val result = executor.submit<String> {
                    InstallationLock(lockFile, onWait = { waiting.countDown() }, sleep = Thread::sleep).withLock { "done" }
                }
                assertTrue(waiting.await(TIMEOUT_SECONDS, TimeUnit.SECONDS), "the lock reports that it waits")
                held.release()
                result
            }
            assertEquals("done", result.get(TIMEOUT_SECONDS, TimeUnit.SECONDS))
        } finally {
            executor.shutdownNow()
        }
    }

    @Test
    fun `a thread waiting for the lock can be interrupted`() {
        val holding = CountDownLatch(1)
        val release = CountDownLatch(1)
        val waiting = CountDownLatch(1)
        val executor = Executors.newFixedThreadPool(2)
        try {
            val holder = executor.submit {
                InstallationLock(lockFile, onWait = {}, sleep = Thread::sleep).withLock {
                    holding.countDown()
                    release.await()
                }
            }
            assertTrue(holding.await(TIMEOUT_SECONDS, TimeUnit.SECONDS))

            val waiterFailure = AtomicReference<Throwable>()
            val waiter = thread {
                try {
                    InstallationLock(lockFile, onWait = { waiting.countDown() }, sleep = Thread::sleep).withLock {
                        error("must not get the lock")
                    }
                } catch (e: Throwable) {
                    waiterFailure.set(e)
                }
            }
            assertTrue(waiting.await(TIMEOUT_SECONDS, TimeUnit.SECONDS))
            waiter.interrupt()
            waiter.join(TimeUnit.SECONDS.toMillis(TIMEOUT_SECONDS))

            // it gave up while the lock was still held
            assertIs<InterruptedException>(waiterFailure.get())
            release.countDown()
            holder.get(TIMEOUT_SECONDS, TimeUnit.SECONDS)

            // the lock is free again for the next caller
            assertEquals("next", InstallationLock(lockFile, onWait = {}, sleep = Thread::sleep).withLock { "next" })
        } finally {
            executor.shutdownNow()
        }
    }

    @Test
    fun `an exception from the action releases the lock`() {
        val lock = InstallationLock(lockFile, onWait = {}, sleep = Thread::sleep)
        val thrown = Executors.newSingleThreadExecutor().let { executor ->
            try {
                executor.submit { lock.withLock { throw IllegalStateException("broken") } }
                    .let { assertFailsWith<ExecutionException> { it.get(TIMEOUT_SECONDS, TimeUnit.SECONDS) } }
            } finally {
                executor.shutdownNow()
            }
        }
        assertIs<IllegalStateException>(thrown.cause)

        assertEquals("again", lock.withLock { "again" })
    }

    private companion object {
        const val TIMEOUT_SECONDS = 30L
    }
}
