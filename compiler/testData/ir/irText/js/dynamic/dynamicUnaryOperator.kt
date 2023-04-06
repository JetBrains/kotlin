// TARGET_BACKEND: JS_IR
// FIR_IDENTICAL

// MUTE_SIGNATURE_COMPARISON_K2: ANY
// ^ KT-57566

fun testUnaryMinus(d: dynamic) = -d
fun testUnaryPlus(d: dynamic) = +d
fun testExcl(d: dynamic) = !d
