/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.caches

import org.jetbrains.kotlin.analysis.test.framework.utils.withDummyApplication
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CleanableSoftValueCacheTest {
    @Test
    fun removeCleansUpValues() {
        val value1 = ValueWithCleanup("v1")
        val value2 = ValueWithCleanup("v2")

        val cache = setUpCache(value1, value2)

        val removedValue1 = cache.remove("v1")
        assertNull(cache.get("v1"))
        assertSame(value1, removedValue1)
        assertTrue(value1.isCleanedUp)

        assertSame(value2, cache.get("v2"))
        assertFalse(value2.isCleanedUp)

        val removedValue2 = cache.remove("v2")
        assertNull(cache.get("v2"))
        assertSame(value2, removedValue2)
        assertTrue(value2.isCleanedUp)
    }

    @Test
    fun putCleansUpValues() {
        val value1 = ValueWithCleanup("v1")
        val value2 = ValueWithCleanup("v2")

        val cache = setUpCache(value1, value2)

        val valueReplacement1 = ValueWithCleanup("vr1")
        val valueReplacement2 = ValueWithCleanup("vr2")

        val oldValue1 = cache.put("v1", valueReplacement1)
        assertSame(valueReplacement1, cache.get("v1"))
        assertSame(value1, oldValue1)
        assertTrue(value1.isCleanedUp)
        assertFalse(valueReplacement1.isCleanedUp)

        assertSame(value2, cache.get("v2"))
        assertFalse(value2.isCleanedUp)

        val oldValue2 = cache.put("v2", valueReplacement2)
        assertSame(valueReplacement2, cache.get("v2"))
        assertSame(value2, oldValue2)
        assertTrue(value2.isCleanedUp)
        assertFalse(valueReplacement2.isCleanedUp)
    }

    @Test
    fun putAvoidsSameValueCleanup() {
        val value1 = ValueWithCleanup("v1")

        val cache = setUpCache(value1)

        val oldValue = cache.put("v1", value1)
        assertSame(value1, cache.get("v1"))
        assertSame(value1, oldValue)
        assertFalse(value1.isCleanedUp)
    }

    @Test
    fun computeAddsValues() {
        val value1 = ValueWithCleanup("v1")

        val cache = setUpCache(value1)

        val value2 = ValueWithCleanup("v2")
        val newValue = cache.compute("v2") { _, oldValue ->
            assertNull(oldValue)
            value2
        }
        assertSame(value2, cache.get("v2"))
        assertSame(value2, newValue)

        assertFalse(value1.isCleanedUp)
        assertFalse(value2.isCleanedUp)
    }

    @Test
    fun computeCleansUpValues() {
        val value1 = ValueWithCleanup("v1")
        val value2 = ValueWithCleanup("v2")

        val cache = setUpCache(value1, value2)

        val valueReplacement1 = ValueWithCleanup("vr1")
        val valueReplacement2: ValueWithCleanup? = null

        val newValue1 = cache.compute("v1") { _, oldValue ->
            assertSame(value1, oldValue)
            valueReplacement1
        }
        assertSame(valueReplacement1, cache.get("v1"))
        assertSame(valueReplacement1, newValue1)
        assertTrue(value1.isCleanedUp)
        assertFalse(valueReplacement1.isCleanedUp)

        assertSame(value2, cache.get("v2"))
        assertFalse(value2.isCleanedUp)

        val newValue2 = cache.compute("v2") { _, oldValue ->
            assertSame(value2, oldValue)
            valueReplacement2
        }
        assertSame(valueReplacement2, cache.get("v2"))
        assertSame(valueReplacement2, newValue2)
        assertTrue(value2.isCleanedUp)
    }

    @Test
    fun computeAvoidsSameValueCleanup() {
        val value1 = ValueWithCleanup("v1")

        val cache = setUpCache(value1)

        val newValue = cache.compute("v1") { _, oldValue ->
            assertSame(value1, oldValue)
            value1
        }
        assertSame(value1, cache.get("v1"))
        assertSame(value1, newValue)
        assertFalse(value1.isCleanedUp)
    }

    @Test
    fun computeIfAbsentAddsValues() {
        val value1 = ValueWithCleanup("v1")

        val cache = setUpCache(value1)

        val value2 = ValueWithCleanup("v2")
        val newValue = cache.computeIfAbsent("v2") { value2 }
        assertSame(value2, cache.get("v2"))
        assertSame(value2, newValue)

        assertFalse(value1.isCleanedUp)
        assertFalse(value2.isCleanedUp)
    }

    @Test
    fun computeIfAbsentKeepsExistingValues() {
        val value1 = ValueWithCleanup("v1")
        val value2 = ValueWithCleanup("v2")

        val cache = setUpCache(value1, value2)

        val valueReplacement1 = ValueWithCleanup("vr1")
        val valueReplacement2 = ValueWithCleanup("vr2")

        val currentValue1 = cache.computeIfAbsent("v1") { valueReplacement1 }
        assertSame(value1, cache.get("v1"))
        assertSame(value1, currentValue1)
        assertFalse(value1.isCleanedUp)
        assertFalse(valueReplacement1.isCleanedUp)

        assertSame(value2, cache.get("v2"))
        assertFalse(value2.isCleanedUp)

        val currentValue2 = cache.computeIfAbsent("v2") { valueReplacement2 }
        assertSame(value2, cache.get("v2"))
        assertSame(value2, currentValue2)
        assertFalse(value2.isCleanedUp)
        assertFalse(valueReplacement2.isCleanedUp)
    }

    @Test
    fun clearCleansUpValues() {
        val value1 = ValueWithCleanup("v1")
        val value2 = ValueWithCleanup("v2")
        val value3 = ValueWithCleanup("v3")

        val cache = setUpCache(value1, value2, value3)

        // We need an application so that `clear` can assert write access.
        withDummyApplication {
            cache.clear()
        }

        assertTrue(cache.isEmpty())
        assertTrue(value1.isCleanedUp)
        assertTrue(value2.isCleanedUp)
        assertTrue(value3.isCleanedUp)
    }

    private fun createCache(): CleanableSoftValueCache<String, ValueWithCleanup> = CleanableSoftValueCache { it.cleanupMarker }

    private fun setUpCache(vararg values: ValueWithCleanup): CleanableSoftValueCache<String, ValueWithCleanup> {
        val cache = createCache()

        values.forEach { value ->
            cache.put(value.name, value)
        }

        cache.keys.forEach { key ->
            val value = cache.get(key)
            assertNotNull(value)
            assertFalse(value!!.isCleanedUp)
        }

        return cache
    }
}
