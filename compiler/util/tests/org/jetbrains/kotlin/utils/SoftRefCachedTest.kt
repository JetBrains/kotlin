/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.utils

import org.junit.jupiter.api.Test
import java.lang.ref.Reference
import java.lang.ref.SoftReference
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.*

class SoftRefCachedTest {

    private class Sample(val id: Int)

    /**
     * Remembers the created soft references, so that the test can simulate their reclamation by the GC.
     */
    private class RecordingSoftRefCached<T>(initializer: () -> T) : AbstractRefCached<T>(initializer) {
        val references = mutableListOf<Reference<Any>>()

        override fun createReference(value: Any): Reference<Any> = SoftReference(value).also { references += it }

        /** Whether the value has been computed and has not been reclaimed yet. */
        fun isValueCached(): Boolean = references.lastOrNull()?.get() != null

        fun simulateGarbageCollection() = references.forEach { it.clear() }
    }

    private val initializerCalls = AtomicInteger()

    private fun sampleCached(): RecordingSoftRefCached<Sample> = RecordingSoftRefCached { Sample(initializerCalls.incrementAndGet()) }

    @Test
    fun `test - value is not computed before the first access`() {
        val cached = sampleCached()

        assertFalse(cached.isValueCached())
        assertEquals("Cached value not computed yet or has been garbage collected.", cached.toString())
        assertEquals(0, initializerCalls.get())
    }

    @Test
    fun `test - value is computed once and cached`() {
        val cached = sampleCached()

        val first = cached.value
        assertEquals(1, first.id)
        assertTrue(cached.isValueCached())

        assertSame(first, cached.value)
        assertSame(first, cached.value)
        assertEquals(1, initializerCalls.get())
        assertEquals(1, cached.references.size)
    }

    @Test
    fun `test - toString returns string representation of the value`() {
        val cached = RecordingSoftRefCached { "hello" }
        cached.value
        assertEquals("hello", cached.toString())
    }

    @Test
    fun `test - value is recomputed after it was garbage collected`() {
        val cached = sampleCached()
        assertEquals(1, cached.value.id)

        cached.simulateGarbageCollection()
        assertFalse(cached.isValueCached())
        assertEquals("Cached value not computed yet or has been garbage collected.", cached.toString())
        assertEquals(1, initializerCalls.get(), "toString() must not trigger the computation")

        val recomputed = cached.value
        assertEquals(2, recomputed.id)
        assertTrue(cached.isValueCached())
        assertSame(recomputed, cached.value)
        assertEquals(2, initializerCalls.get())
    }

    @Test
    fun `test - value is recomputed every time it was garbage collected`() {
        val cached = sampleCached()
        repeat(5) { iteration ->
            assertEquals(iteration + 1, cached.value.id)
            cached.simulateGarbageCollection()
        }
        assertEquals(5, initializerCalls.get())
    }

    @Test
    fun `test - null value is cached`() {
        val cached = RecordingSoftRefCached { initializerCalls.incrementAndGet(); null }

        assertFalse(cached.isValueCached())
        assertNull(cached.value)
        assertTrue(cached.isValueCached())
        assertEquals("null", cached.toString())

        assertNull(cached.value)
        assertEquals(1, initializerCalls.get())
    }

    @Test
    fun `test - null value is recomputed after it was garbage collected`() {
        val cached = RecordingSoftRefCached { initializerCalls.incrementAndGet(); null }
        assertNull(cached.value)

        cached.simulateGarbageCollection()
        assertFalse(cached.isValueCached())

        assertNull(cached.value)
        assertEquals(2, initializerCalls.get())
    }

    @Test
    fun `test - exception from initializer is propagated and the computation is retried`() {
        val cached = RecordingSoftRefCached {
            val call = initializerCalls.incrementAndGet()
            if (call == 1) throw IllegalStateException("Failure #$call")
            Sample(call)
        }

        val exception = assertFailsWith<IllegalStateException> { cached.value }
        assertEquals("Failure #1", exception.message)
        assertFalse(cached.isValueCached())
        assertTrue(cached.references.isEmpty())

        assertEquals(2, cached.value.id)
        assertTrue(cached.isValueCached())
        assertEquals(2, initializerCalls.get())
    }

    @Test
    fun `test - concurrent access computes the value once`() {
        val threads = 16
        val startLatch = CountDownLatch(1)
        val cached = RecordingSoftRefCached {
            Thread.sleep(50) // Give other threads a chance to observe the missing value.
            Sample(initializerCalls.incrementAndGet())
        }

        val executor = Executors.newFixedThreadPool(threads)
        try {
            val futures = List(threads) {
                executor.submit<Sample> {
                    startLatch.await()
                    cached.value
                }
            }
            startLatch.countDown()

            val results = futures.map { it.get(10, TimeUnit.SECONDS) }
            assertEquals(1, initializerCalls.get())
            results.forEach { assertSame(results.first(), it) }
        } finally {
            executor.shutdownNow()
        }
    }

    @Test
    fun `test - concurrent access after garbage collection recomputes the value once`() {
        val threads = 16
        val cached = sampleCached()
        assertEquals(1, cached.value.id)
        cached.simulateGarbageCollection()

        val startLatch = CountDownLatch(1)
        val executor = Executors.newFixedThreadPool(threads)
        try {
            val futures = List(threads) {
                executor.submit<Sample> {
                    startLatch.await()
                    cached.value
                }
            }
            startLatch.countDown()

            val results = futures.map { it.get(10, TimeUnit.SECONDS) }
            assertEquals(2, initializerCalls.get())
            results.forEach { assertSame(results.first(), it) }
        } finally {
            executor.shutdownNow()
        }
    }

    @Test
    fun `test - softRefCached as property delegate`() {
        class Holder {
            val sample: Sample by softRefCached { Sample(initializerCalls.incrementAndGet()) }
        }

        val holder = Holder()
        assertEquals(0, initializerCalls.get())
        val first = holder.sample
        assertEquals(1, first.id)
        assertSame(first, holder.sample)
        assertEquals(1, initializerCalls.get())
    }

    @Test
    fun `test - strongly reachable value survives garbage collection`() {
        val cached = softRefCached { Sample(initializerCalls.incrementAndGet()) }

        val stronglyHeld = cached.value
        System.gc()

        assertSame(stronglyHeld, cached.value)
        assertEquals(1, initializerCalls.get())
    }
}
