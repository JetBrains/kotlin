/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.caches

import org.jetbrains.kotlin.analysis.low.level.api.fir.caches.cleanable.CleanableSoftValueReferenceCache
import org.jetbrains.kotlin.analysis.low.level.api.fir.caches.cleanable.CleanableValueReferenceCache
import org.jetbrains.kotlin.analysis.low.level.api.fir.caches.cleanable.CleanableWeakValueReferenceCache
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

private typealias Cache = CleanableValueReferenceCache<String, ValueWithCleanup>

class CleanableValueReferenceCacheTest {
    companion object {
        @JvmStatic
        private fun createCaches(): Stream<Cache> = Stream.of(
            CleanableWeakValueReferenceCache { it.cleanupMarker },
            CleanableSoftValueReferenceCache { it.cleanupMarker },
        )
    }

    @ParameterizedTest
    @MethodSource("createCaches")
    fun removeCleansUpValues(cache: Cache) {
        val value1 = ValueWithCleanup("v1")
        val value2 = ValueWithCleanup("v2")

        cache.setUp(value1, value2)

        val removedValue1 = cache.remove("v1")
        assertNull(cache["v1"])
        assertSame(value1, removedValue1)
        assertTrue(value1.isCleanedUp)

        assertSame(value2, cache["v2"])
        assertFalse(value2.isCleanedUp)

        val removedValue2 = cache.remove("v2")
        assertNull(cache["v2"])
        assertSame(value2, removedValue2)
        assertTrue(value2.isCleanedUp)
    }

    @ParameterizedTest
    @MethodSource("createCaches")
    fun putCleansUpValues(cache: Cache) {
        val value1 = ValueWithCleanup("v1")
        val value2 = ValueWithCleanup("v2")

        cache.setUp(value1, value2)

        val valueReplacement1 = ValueWithCleanup("vr1")
        val valueReplacement2 = ValueWithCleanup("vr2")

        val oldValue1 = cache.put("v1", valueReplacement1)
        assertSame(valueReplacement1, cache["v1"])
        assertSame(value1, oldValue1)
        assertTrue(value1.isCleanedUp)
        assertFalse(valueReplacement1.isCleanedUp)

        assertSame(value2, cache["v2"])
        assertFalse(value2.isCleanedUp)

        val oldValue2 = cache.put("v2", valueReplacement2)
        assertSame(valueReplacement2, cache["v2"])
        assertSame(value2, oldValue2)
        assertTrue(value2.isCleanedUp)
        assertFalse(valueReplacement2.isCleanedUp)
    }

    @ParameterizedTest
    @MethodSource("createCaches")
    fun putAvoidsSameValueCleanup(cache: Cache) {
        val value1 = ValueWithCleanup("v1")

        cache.setUp(value1)

        val oldValue = cache.put("v1", value1)
        assertSame(value1, cache["v1"])
        assertSame(value1, oldValue)
        assertFalse(value1.isCleanedUp)
    }

    @ParameterizedTest
    @MethodSource("createCaches")
    fun computeAddsValues(cache: Cache) {
        val value1 = ValueWithCleanup("v1")

        cache.setUp(value1)

        val value2 = ValueWithCleanup("v2")
        val newValue = cache.compute("v2") { _, oldValue ->
            assertNull(oldValue)
            value2
        }
        assertSame(value2, cache["v2"])
        assertSame(value2, newValue)

        assertFalse(value1.isCleanedUp)
        assertFalse(value2.isCleanedUp)
    }

    @ParameterizedTest
    @MethodSource("createCaches")
    fun computeCleansUpValues(cache: Cache) {
        val value1 = ValueWithCleanup("v1")
        val value2 = ValueWithCleanup("v2")

        cache.setUp(value1, value2)

        val valueReplacement1 = ValueWithCleanup("vr1")
        val valueReplacement2: ValueWithCleanup? = null

        val newValue1 = cache.compute("v1") { _, oldValue ->
            assertSame(value1, oldValue)
            valueReplacement1
        }
        assertSame(valueReplacement1, cache["v1"])
        assertSame(valueReplacement1, newValue1)
        assertTrue(value1.isCleanedUp)
        assertFalse(valueReplacement1.isCleanedUp)

        assertSame(value2, cache["v2"])
        assertFalse(value2.isCleanedUp)

        val newValue2 = cache.compute("v2") { _, oldValue ->
            assertSame(value2, oldValue)
            valueReplacement2
        }
        assertSame(valueReplacement2, cache["v2"])
        assertSame(valueReplacement2, newValue2)
        assertTrue(value2.isCleanedUp)
    }

    @ParameterizedTest
    @MethodSource("createCaches")
    fun computeAvoidsSameValueCleanup(cache: Cache) {
        val value1 = ValueWithCleanup("v1")

        cache.setUp(value1)

        val newValue = cache.compute("v1") { _, oldValue ->
            assertSame(value1, oldValue)
            value1
        }
        assertSame(value1, cache["v1"])
        assertSame(value1, newValue)
        assertFalse(value1.isCleanedUp)
    }

    @ParameterizedTest
    @MethodSource("createCaches")
    fun computeIfAbsentAddsValues(cache: Cache) {
        val value1 = ValueWithCleanup("v1")

        cache.setUp(value1)

        val value2 = ValueWithCleanup("v2")
        val newValue = cache.computeIfAbsent("v2") { value2 }
        assertSame(value2, cache["v2"])
        assertSame(value2, newValue)

        assertFalse(value1.isCleanedUp)
        assertFalse(value2.isCleanedUp)
    }

    @ParameterizedTest
    @MethodSource("createCaches")
    fun computeIfAbsentKeepsExistingValues(cache: Cache) {
        val value1 = ValueWithCleanup("v1")
        val value2 = ValueWithCleanup("v2")

        cache.setUp(value1, value2)

        val valueReplacement1 = ValueWithCleanup("vr1")
        val valueReplacement2 = ValueWithCleanup("vr2")

        val currentValue1 = cache.computeIfAbsent("v1") { valueReplacement1 }
        assertSame(value1, cache["v1"])
        assertSame(value1, currentValue1)
        assertFalse(value1.isCleanedUp)
        assertFalse(valueReplacement1.isCleanedUp)

        assertSame(value2, cache["v2"])
        assertFalse(value2.isCleanedUp)

        val currentValue2 = cache.computeIfAbsent("v2") { valueReplacement2 }
        assertSame(value2, cache["v2"])
        assertSame(value2, currentValue2)
        assertFalse(value2.isCleanedUp)
        assertFalse(valueReplacement2.isCleanedUp)
    }

    @ParameterizedTest
    @MethodSource("createCaches")
    fun clearCleansUpValues(cache: Cache) {
        val value1 = ValueWithCleanup("v1")
        val value2 = ValueWithCleanup("v2")
        val value3 = ValueWithCleanup("v3")

        cache.setUp(value1, value2, value3)

        cache.clear()

        assertTrue(cache.isEmpty())
        assertTrue(value1.isCleanedUp)
        assertTrue(value2.isCleanedUp)
        assertTrue(value3.isCleanedUp)
    }

    private fun Cache.setUp(vararg values: ValueWithCleanup) {
        values.forEach { value ->
            put(value.name, value)
        }

        keys.forEach { key ->
            val value = get(key)
            assertNotNull(value)
            assertFalse(value!!.isCleanedUp)
        }
    }
}
