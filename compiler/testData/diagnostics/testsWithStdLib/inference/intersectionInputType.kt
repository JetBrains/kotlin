// FIR_IDENTICAL
// !LANGUAGE: +NewInference

import kotlin.test.assertEquals

fun test() {
    val u = when (true) {
        true -> 42
        else -> 1.0
    }

    assertEquals(42, u)
}