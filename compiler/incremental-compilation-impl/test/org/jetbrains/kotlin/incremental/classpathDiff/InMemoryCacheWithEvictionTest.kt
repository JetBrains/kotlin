/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental.classpathDiff

import com.google.common.util.concurrent.AtomicDouble
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.fail

class InMemoryCacheWithEvictionTest {

    @Test
    fun testComputeIfAbsent() {
        val cache = InMemoryCacheWithEviction<Int, Any>(maxTimePeriods = 10, maxMemoryUsageRatio = 1.0, memoryUsageRatio = { 0.5 })

        // Check when the entries are not yet present
        assertEquals("One", cache.computeIfAbsent(1) { "One" })
        assertEquals("Two", cache.computeIfAbsent(2) { "Two" })

        // Check when the entries are already present
        assertEquals("One", cache.computeIfAbsent(1) { fail("Must not run") })
        assertEquals("Two", cache.computeIfAbsent(2) { fail("Must not run") })
    }

    @Test
    fun testLeastRecentlyUsedCacheEviction() {
        val cache = InMemoryCacheWithEviction<Int, Any>(maxTimePeriods = 2, maxMemoryUsageRatio = 1.0, memoryUsageRatio = { 0.5 })

        // Time period 0 - Cache entry 0 is added, no cache entries are evicted
        cache.computeIfAbsent(0) { "Zero" }
        cache.evictEntries()
        assertFalse(cache.keyWasEvicted(0))

        // Time period 1 - Cache entry 1 is added, no cache entries are evicted
        cache.newTimePeriod()
        cache.computeIfAbsent(1) { "One" }
        cache.evictEntries()
        assertFalse(cache.keyWasEvicted(0))
        assertFalse(cache.keyWasEvicted(1))

        // Time period 2 - Cache entry 2 is added, cache entry 0 is evicted (because maxTimePeriods = 2)
        cache.newTimePeriod()
        cache.computeIfAbsent(2) { "Two" }
        cache.evictEntries()
        assertTrue(cache.keyWasEvicted(0))
        assertFalse(cache.keyWasEvicted(1))
        assertFalse(cache.keyWasEvicted(2))

        // Time period 3 - Cache entry 1 is evicted
        cache.newTimePeriod()
        cache.evictEntries()
        assertTrue(cache.keyWasEvicted(0))
        assertTrue(cache.keyWasEvicted(1))
        assertFalse(cache.keyWasEvicted(2))

        // Time period 4 - Cache entry 2 is evicted
        cache.newTimePeriod()
        cache.evictEntries()
        assertTrue(cache.keyWasEvicted(0))
        assertTrue(cache.keyWasEvicted(1))
        assertTrue(cache.keyWasEvicted(2))
    }

    @Test
    fun testMemoryUsageLimitCacheEviction() {
        val memoryUsageRatio = AtomicDouble(0.5)
        val cache = InMemoryCacheWithEviction<Int, Any>(
            maxTimePeriods = 10,
            maxMemoryUsageRatio = 0.8,
            memoryUsageRatio = { memoryUsageRatio.get() }
        )

        // Time period 0 - Cache entry 0 is added, no cache entries are evicted
        cache.computeIfAbsent(0) { "Zero" }
        cache.evictEntries()
        assertFalse(cache.keyWasEvicted(0))

        // Time period 1 - Cache entry 1 is added, no cache entries are evicted
        cache.newTimePeriod()
        cache.computeIfAbsent(1) { "One" }
        cache.evictEntries()
        assertFalse(cache.keyWasEvicted(0))
        assertFalse(cache.keyWasEvicted(1))

        // Memory usage increases to above the limit (maxMemoryUsageRatio = 0.8)
        memoryUsageRatio.set(0.9)

        // Time period 2 - Cache entry 2 is added, all cache entries are evicted
        cache.newTimePeriod()
        cache.computeIfAbsent(2) { "Two" }
        cache.evictEntries()
        assertTrue(cache.keyWasEvicted(0))
        assertTrue(cache.keyWasEvicted(1))
        assertTrue(cache.keyWasEvicted(2))

        // Memory usage decreases back to below the limit (maxMemoryUsageRatio = 0.8)
        memoryUsageRatio.set(0.5)

        // Time period 3 - Cache entry 3 is added, again no cache entries are evicted
        cache.newTimePeriod()
        cache.computeIfAbsent(3) { "Three" }
        cache.evictEntries()
        assertFalse(cache.keyWasEvicted(3))
    }
}
