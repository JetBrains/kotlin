/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental.classpathDiff

import com.google.common.util.concurrent.AtomicDouble
import org.jetbrains.kotlin.incremental.classpathDiff.InMemoryCacheWithEviction.EntryState.*
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.fail

class InMemoryCacheWithEvictionTest {

    @Test
    fun testComputeIfAbsent() {
        val cache = InMemoryCacheWithEviction<Int, Any>(
            maxTimePeriodsToKeepStrongReferences = 10,
            maxTimePeriodsToKeepSoftReferences = 10,
            maxMemoryUsageRatioToKeepStrongReferences = 0.8,
            memoryUsageRatio = { 0.5 }
        )

        // Check when the entries are not yet present
        assertEquals("One", cache.computeIfAbsent(1) { "One" })
        assertEquals("Two", cache.computeIfAbsent(2) { "Two" })

        // Check when the entries are already present
        assertEquals("One", cache.computeIfAbsent(1) { fail("Must not run") })
        assertEquals("Two", cache.computeIfAbsent(2) { fail("Must not run") })
    }

    @Test
    fun testLeastRecentlyUsedCacheEviction() {
        val cache = InMemoryCacheWithEviction<Int, Any>(
            maxTimePeriodsToKeepStrongReferences = 2,
            maxTimePeriodsToKeepSoftReferences = 3,
            maxMemoryUsageRatioToKeepStrongReferences = 0.8,
            memoryUsageRatio = { 0.5 }
        )

        // Check that cache entries change states at different time periods depending on when they were last used
        cache.newTimePeriod()
        cache.computeIfAbsent(1) { "One" }
        cache.computeIfAbsent(2) { "Two" }
        cache.computeIfAbsent(3) { "Three" }
        cache.evictEntries()
        assertEquals(STRONG_REF, cache.getEntryState(1))
        assertEquals(STRONG_REF, cache.getEntryState(2))
        assertEquals(STRONG_REF, cache.getEntryState(3))

        cache.newTimePeriod()
        cache.computeIfAbsent(2) { fail("Must not run") }
        cache.evictEntries()
        assertEquals(STRONG_REF, cache.getEntryState(1))
        assertEquals(STRONG_REF, cache.getEntryState(2))
        assertEquals(STRONG_REF, cache.getEntryState(3))

        cache.newTimePeriod()
        cache.computeIfAbsent(3) { fail("Must not run") }
        cache.evictEntries()
        assertEquals(SOFT_REF, cache.getEntryState(1))
        assertEquals(STRONG_REF, cache.getEntryState(2))
        assertEquals(STRONG_REF, cache.getEntryState(3))

        cache.newTimePeriod()
        cache.evictEntries()
        assertEquals(SOFT_REF, cache.getEntryState(1))
        assertEquals(SOFT_REF, cache.getEntryState(2))
        assertEquals(STRONG_REF, cache.getEntryState(3))

        cache.newTimePeriod()
        cache.evictEntries()
        assertEquals(SOFT_REF, cache.getEntryState(1))
        assertEquals(SOFT_REF, cache.getEntryState(2))
        assertEquals(SOFT_REF, cache.getEntryState(3))

        cache.newTimePeriod()
        cache.evictEntries()
        assertEquals(ABSENT, cache.getEntryState(1))
        assertEquals(SOFT_REF, cache.getEntryState(2))
        assertEquals(SOFT_REF, cache.getEntryState(3))

        cache.newTimePeriod()
        cache.evictEntries()
        assertEquals(ABSENT, cache.getEntryState(1))
        assertEquals(ABSENT, cache.getEntryState(2))
        assertEquals(SOFT_REF, cache.getEntryState(3))

        cache.newTimePeriod()
        cache.evictEntries()
        assertEquals(ABSENT, cache.getEntryState(1))
        assertEquals(ABSENT, cache.getEntryState(2))
        assertEquals(ABSENT, cache.getEntryState(3))
    }

    @Test
    fun testMemoryUsageLimitCacheEviction() {
        val memoryUsageRatio = AtomicDouble(0.5)
        val cache = InMemoryCacheWithEviction<Int, Any>(
            maxTimePeriodsToKeepStrongReferences = 10,
            maxTimePeriodsToKeepSoftReferences = 10,
            maxMemoryUsageRatioToKeepStrongReferences = 0.8,
            memoryUsageRatio = { memoryUsageRatio.get() }
        )

        // Time period 0 - Cache entry 0 is added, no cache entries are evicted
        cache.computeIfAbsent(0) { "Zero" }
        cache.evictEntries()
        assertEquals(STRONG_REF, cache.getEntryState(0))

        // Time period 1 - Cache entry 1 is added, no cache entries are evicted
        cache.newTimePeriod()
        cache.computeIfAbsent(1) { "One" }
        cache.evictEntries()
        assertEquals(STRONG_REF, cache.getEntryState(0))
        assertEquals(STRONG_REF, cache.getEntryState(1))

        // Memory usage increases to above the limit (0.8)
        memoryUsageRatio.set(0.9)

        // Time period 2 - Cache entry 2 is added, all cache entries are evicted
        cache.newTimePeriod()
        cache.computeIfAbsent(2) { "Two" }
        cache.evictEntries()
        assertEquals(SOFT_REF, cache.getEntryState(0))
        assertEquals(SOFT_REF, cache.getEntryState(1))
        assertEquals(SOFT_REF, cache.getEntryState(2))

        // Memory usage decreases back to below the limit (0.8)
        memoryUsageRatio.set(0.5)

        // Time period 3 - Cache entry 3 is added, again no cache entries are evicted
        cache.newTimePeriod()
        cache.computeIfAbsent(3) { "Three" }
        cache.evictEntries()
        assertEquals(STRONG_REF, cache.getEntryState(3))
    }
}
