/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("UNUSED_PARAMETER")

package org.jetbrains.kotlin.util

import org.jetbrains.kotlin.utils.tryCreateCallableMappingFromNamedArgs
import org.jetbrains.kotlin.utils.tryCreateCallableMappingFromStringArgs
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import kotlin.reflect.KParameter

class ArgsToParamsMatchingTest {
    @Test
    fun testMatchFromStrings() {
        Assertions.assertNull(tryCreateCallableMappingFromStringArgs(::foo, listOf()))
        Assertions.assertNull(tryCreateCallableMappingFromStringArgs(::foo, listOf("1", "2")))

        assertParamMapsEquals(
            tryCreateCallableMappingFromStringArgs(::foo, listOf("1", "2", "s", "0.1")),
            "i" to 1, "b" to 2.toByte(), "c" to 's', "d" to 0.1
        )

        Assertions.assertNull(tryCreateCallableMappingFromStringArgs(::foo, listOf("1", "258", "s", "0.1")))
        Assertions.assertNull(tryCreateCallableMappingFromStringArgs(::foo, listOf("1", "258", "s", "0")))
        Assertions.assertNull(tryCreateCallableMappingFromStringArgs(::foo, listOf("1", "258", "sss", "0.1")))

        assertParamMapsEquals(
            tryCreateCallableMappingFromStringArgs(
                ::foo,
                listOf("1", "2", "s", "0.1", "abc", "true", "1", "2", "3")
            ),
            "i" to 1, "b" to 2.toByte(), "c" to 's', "d" to 0.1, "s" to "abc", "t" to true, "v" to arrayOf(1L, 2L, 3L)
        )

        Assertions.assertNull(tryCreateCallableMappingFromStringArgs(::foo, listOf("i", "b", "c")))
        Assertions.assertNull(
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
        Assertions.assertNull(tryCreateCallableMappingFromStringArgs(::charArray, listOf("")))

        assertParamMapsEquals(
            tryCreateCallableMappingFromStringArgs(::varargStrings, listOf("a", "b", "c")),
            "s" to arrayOf("a", "b", "c")
        )
    }

    @Test
    fun testMatchNamed() {
        Assertions.assertNull(tryCreateCallableMappingFromNamedArgs(::foo, listOf()))
        Assertions.assertNull(tryCreateCallableMappingFromNamedArgs(::foo, listOf(null to 1, null to 2)))

        assertParamMapsEquals(
            tryCreateCallableMappingFromNamedArgs(
                ::foo,
                listOf(null to 1, null to 2.toByte(), null to 's', null to 0.1)
            ),
            "i" to 1, "b" to 2.toByte(), "c" to 's', "d" to 0.1
        )

        assertParamMapsEquals(
            tryCreateCallableMappingFromNamedArgs(
                ::foo,
                listOf(null to 1, null to 2.toByte(), "c" to 's', "d" to 0.1)
            ),
            "i" to 1, "b" to 2.toByte(), "c" to 's', "d" to 0.1
        )

        assertParamMapsEquals(
            tryCreateCallableMappingFromNamedArgs(
                ::foo,
                listOf(null to 1, null to 2.toByte(), "d" to 0.1, "c" to 's')
            ),
            "i" to 1, "b" to 2.toByte(), "c" to 's', "d" to 0.1
        )

        assertParamMapsEquals(
            tryCreateCallableMappingFromNamedArgs(
                ::foo,
                listOf(
                    null to 1,
                    null to 2.toByte(),
                    null to 's',
                    null to 0.1,
                    "v" to longArrayOf(1L, 2L, 3L)
                )
            ),
            "i" to 1, "b" to 2.toByte(), "c" to 's', "d" to 0.1, "v" to longArrayOf(1L, 2L, 3L)
        )

        Assertions.assertNull(
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
        Assertions.assertNull(
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

        Assertions.assertNull(tryCreateCallableMappingFromNamedArgs(::notNullNumber, listOf(null to null)))
        assertParamMapsEquals(
            tryCreateCallableMappingFromNamedArgs(::nullableNumber, listOf(null to null)),
            "n" to null
        )
        assertParamMapsEquals(
            tryCreateCallableMappingFromNamedArgs(::notNullNumber, listOf(null to 42)),
            "n" to 42
        )
        Assertions.assertNull(tryCreateCallableMappingFromNamedArgs(::notNullNumber, listOf(null to "42")))

        assertParamMapsEquals(
            tryCreateCallableMappingFromNamedArgs(::varargStrings, listOf("a", "b", "c").map { null to it }),
            "s" to arrayOf("a", "b", "c")
        )
    }
}

private fun assertParamMapsEquals(actuals: Map<KParameter, Any?>?, vararg expected: Pair<String, Any?>) {
    Assertions.assertNotNull(actuals)
    val stringifiedActuals = actuals!!.mapKeys { it.key.name }
    val mappedExpected = expected.toMap()
    if (mappedExpected != stringifiedActuals) {
        Assertions.assertEquals(stringifiedActuals.keys, mappedExpected.keys)
        mappedExpected.forEach { exp ->
            val actVal = stringifiedActuals[exp.key]
            if (exp.value != actVal) {
                val msg = "Unexpected value for key '${exp.key}'; expected: ${exp.value}, actual: $actVal"
                if ((exp.value?.javaClass?.isArray ?: false) && (actVal?.javaClass?.isArray ?: false)) {
                    Assertions.assertArrayEquals(
                        arrayOf(exp.value),
                        arrayOf(actVal),
                        msg
                    ) // tricking Array.deepEquals to compare single element arrays (instead of tedious casting to typed array)
                } else {
                    Assertions.assertEquals(exp.value, actVal, msg)
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
