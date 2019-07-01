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

@file:Suppress("UNUSED_PARAMETER")

package org.jetbrains.kotlin.util

import org.jetbrains.kotlin.utils.tryCreateCallableMappingFromNamedArgs
import org.jetbrains.kotlin.utils.tryCreateCallableMappingFromStringArgs
import org.junit.Assert
import org.junit.Test
import kotlin.reflect.KParameter

class ArgsToParamsMatchingTest {
    @Test
    fun testMatchFromStrings() {
        Assert.assertNull(tryCreateCallableMappingFromStringArgs(::foo, listOf()))
        Assert.assertNull(tryCreateCallableMappingFromStringArgs(::foo, listOf("1", "2")))

        assertParamMapsEquals(
            tryCreateCallableMappingFromStringArgs(::foo, listOf("1", "2", "s", "0.1")),
            "i" to 1, "b" to 2.toByte(), "c" to 's', "d" to 0.1)

        Assert.assertNull(tryCreateCallableMappingFromStringArgs(::foo, listOf("1", "258", "s", "0.1")))
        Assert.assertNull(tryCreateCallableMappingFromStringArgs(::foo, listOf("1", "258", "s", "0")))
        Assert.assertNull(tryCreateCallableMappingFromStringArgs(::foo, listOf("1", "258", "sss", "0.1")))

        assertParamMapsEquals(
            tryCreateCallableMappingFromStringArgs(
                ::foo,
                listOf("1", "2", "s", "0.1", "abc", "true", "1", "2", "3")
            ),
            "i" to 1, "b" to 2.toByte(), "c" to 's', "d" to 0.1, "s" to "abc", "t" to true, "v" to arrayOf(1L, 2L, 3L))

        Assert.assertNull(tryCreateCallableMappingFromStringArgs(::foo, listOf("i", "b", "c")))
        Assert.assertNull(
            tryCreateCallableMappingFromStringArgs(
                ::foo,
                listOf(
                    "1",
                    "2",
                    "s",
                    "0.1",
                    "abc",
                    "true",
                    "not-a-long"
                )
            )
        )
        Assert.assertNull(tryCreateCallableMappingFromStringArgs(::charArray, listOf("")))

        assertParamMapsEquals(
            tryCreateCallableMappingFromStringArgs(::varargStrings, listOf("a", "b", "c")),
            "s" to arrayOf("a", "b", "c"))
    }

    @Test
    fun testMatchNamed() {
        Assert.assertNull(tryCreateCallableMappingFromNamedArgs(::foo, listOf()))
        Assert.assertNull(tryCreateCallableMappingFromNamedArgs(::foo, listOf(null to 1, null to 2)))

        assertParamMapsEquals(
            tryCreateCallableMappingFromNamedArgs(
                ::foo,
                listOf(null to 1, null to 2.toByte(), null to 's', null to 0.1)
            ),
            "i" to 1, "b" to 2.toByte(), "c" to 's', "d" to 0.1)

        assertParamMapsEquals(
            tryCreateCallableMappingFromNamedArgs(
                ::foo,
                listOf(null to 1, null to 2.toByte(), "c" to 's', "d" to 0.1)
            ),
            "i" to 1, "b" to 2.toByte(), "c" to 's', "d" to 0.1)

        assertParamMapsEquals(
            tryCreateCallableMappingFromNamedArgs(
                ::foo,
                listOf(null to 1, null to 2.toByte(), "d" to 0.1, "c" to 's')
            ),
            "i" to 1, "b" to 2.toByte(), "c" to 's', "d" to 0.1)

        assertParamMapsEquals(
            tryCreateCallableMappingFromNamedArgs(
                ::foo,
                listOf(
                    null to 1,
                    null to 2.toByte(),
                    null to 's',
                    null to 0.1,
                    "v" to arrayOf(1L, 2L, 3L)
                )
            ),
            "i" to 1, "b" to 2.toByte(), "c" to 's', "d" to 0.1, "v" to arrayOf(1L, 2L, 3L))

        Assert.assertNull(
            tryCreateCallableMappingFromNamedArgs(
                ::foo,
                listOf(
                    null to 1,
                    null to 2.toByte(),
                    null to 's',
                    "x" to 0.1
                )
            )
        ) // wrong name
        Assert.assertNull(
            tryCreateCallableMappingFromNamedArgs(
                ::foo,
                listOf(
                    null to 1,
                    null to 2.toByte(),
                    "c" to 's',
                    null to 0.1
                )
            )
        ) // unnamed after named

        Assert.assertNull(tryCreateCallableMappingFromNamedArgs(::notNullNumber, listOf(null to null)))
        assertParamMapsEquals(
            tryCreateCallableMappingFromNamedArgs(::nullableNumber, listOf(null to null)),
            "n" to null)
        assertParamMapsEquals(
            tryCreateCallableMappingFromNamedArgs(::notNullNumber, listOf(null to 42)),
            "n" to 42)
        Assert.assertNull(tryCreateCallableMappingFromNamedArgs(::notNullNumber, listOf(null to "42")))

        assertParamMapsEquals(
            tryCreateCallableMappingFromNamedArgs(::varargStrings, listOf("a", "b", "c").map { null to it }),
            "s" to arrayOf("a", "b", "c"))
    }
}

private fun assertParamMapsEquals(actuals: Map<KParameter, Any?>?, vararg expected: Pair<String, Any?>) {
    Assert.assertNotNull(actuals)
    val stringifiedActuals = actuals!!.mapKeys { it.key.name }
    val mappedExpected = expected.toMap()
    if (expected != stringifiedActuals) {
        Assert.assertEquals(stringifiedActuals.keys, mappedExpected.keys)
        mappedExpected.forEach { exp ->
            val actVal = stringifiedActuals[exp.key]
            if (exp.value != actVal) {
                val msg = "Unexpected value for key '${exp.key}'; expected: ${exp.value}, actual: $actVal"
                if ((exp.value?.javaClass?.isArray ?: false) && (actVal?.javaClass?.isArray ?: false )) {
                    Assert.assertArrayEquals(msg, arrayOf(exp.value), arrayOf(actVal)) // tricking Array.deepEquals to compare single element arrays (instead of tedious casting to typed array)
                }
                else {
                    Assert.assertEquals(msg, exp.value, actVal)
                }
            }
        }
    }
}

private fun foo(i: Int, b: Byte, c: Char, d: Double = 0.0, s: String = "", t: Boolean = true, vararg v: Long) {}

private fun charArray(c: CharArray) {}

private fun varargStrings(vararg s: String) {}

private fun notNullNumber(n: Number) {}

private fun nullableNumber(n: Number?) {}
