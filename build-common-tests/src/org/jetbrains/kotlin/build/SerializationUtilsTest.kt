/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.build

import junit.framework.TestCase
import org.junit.Test

class SerializationUtilsTest : TestCase() {
    data class TestPropertyTypes(
            val intNull: Int?,
            val int: Int,
            val stringNull: String?,
            val string: String,
            val boolNull: Boolean?,
            val bool: Boolean
    )

    @Test
    fun testPropertyTypes() {
        val instance1 = TestPropertyTypes(null, 1, null, "abc", null, false)
        val deserialized1 = deserializeFromPlainText<TestPropertyTypes>(serializeToPlainText(instance1))
        assertEquals(instance1, deserialized1)

        val instance2 = TestPropertyTypes(1, 2, "abc", "xyz", true, false)
        val deserialized2 = deserializeFromPlainText<TestPropertyTypes>(serializeToPlainText(instance2))
        assertEquals(instance2, deserialized2)
    }

    data class TestAddedField1(val x: Int)
    data class TestAddedField2(val x: Int, val y: Int?)

    @Test
    fun testAddedField() {
        val testAddedField1 = TestAddedField1(1)
        val serialized = serializeToPlainText(testAddedField1)
        val deserialized = deserializeFromPlainText<TestAddedField2>(serialized)

        assertEquals(TestAddedField2(1, null), deserialized)
    }
}

