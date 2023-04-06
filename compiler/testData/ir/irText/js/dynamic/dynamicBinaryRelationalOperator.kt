// TARGET_BACKEND: JS_IR
// FIR_IDENTICAL

// MUTE_SIGNATURE_COMPARISON_K2: ANY
// ^ KT-57566

fun testLess(d: dynamic) = d < 2
fun testLessOrEqual(d: dynamic) = d <= 2
fun testGreater(d: dynamic) = d > 2
fun testGreaterOrEqual(d: dynamic) = d >= 2
