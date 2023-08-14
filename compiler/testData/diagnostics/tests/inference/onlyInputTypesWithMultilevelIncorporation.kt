// WITH_STDLIB
// !DIAGNOSTICS: -UNUSED_PARAMETER

@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

import kotlin.internal.OnlyInputTypes

fun <@OnlyInputTypes T> assertEquals(expected: T, actual: T, message: String? = null) {}

fun main() {
    assertEquals(
        mapOf(1 to "1", 2 to "2", 3 to "3"),
        intArrayOf(1, 2, 3).associateWith { it.toString() }
    )
}