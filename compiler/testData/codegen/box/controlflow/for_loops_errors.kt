// WITH_STDLIB

import kotlin.test.*

fun box(): String {
    // Negative step.
    try {
        for (i in 0 .. 4 step -2) return "FAIL 1 $i"
        throw AssertionError()
    } catch (e: IllegalArgumentException) {}

    try {
        for (i in 0 until 4 step -2) return "FAIL 2 $i"
        throw AssertionError()
    } catch (e: IllegalArgumentException) {}

    try {
        for (i in 4 downTo 0 step -2) return "FAIL 3 $i"
        throw AssertionError()
    } catch (e: IllegalArgumentException) {}

    // Zero step.
    try {
        for (i in 0 .. 4 step 0) return "FAIL 4 $i"
        throw AssertionError()
    } catch (e: IllegalArgumentException) {}

    try {
        for (i in 0 until 4 step 0) return "FAIL 5 $i"
        throw AssertionError()
    } catch (e: IllegalArgumentException) {}

    try {
        for (i in 4 downTo 0 step 0) return "FAIL 6 $i"
        throw AssertionError()
    } catch (e: IllegalArgumentException) {}

    // Two steps, one is negative.
    try {
        for (i in 0 .. 4 step -2 step 3) return "FAIL 7 $i"
        throw AssertionError()
    } catch (e: IllegalArgumentException) {}

    try {
        for (i in 0 until 4 step -2 step 3) return "FAIL 8 $i"
        throw AssertionError()
    } catch (e: IllegalArgumentException) {}

    try {
        for (i in 4 downTo 0 step -2 step 3) return "FAIL 9 $i"
        throw AssertionError()
    } catch (e: IllegalArgumentException) {}

    return "OK"
}
