/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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

