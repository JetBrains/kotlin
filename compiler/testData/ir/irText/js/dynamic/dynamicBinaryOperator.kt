// TARGET_BACKEND: JS_IR
// FIR_IDENTICAL

// MUTE_SIGNATURE_COMPARISON_K2: ANY
// ^ KT-57566

fun testBinaryPlus(d: dynamic) = d + 1
fun testBinaryMinus(d: dynamic) = d - 1
fun testMul(d: dynamic) = d * 2
fun testDiv(d: dynamic) = d / 2
fun testMod(d: dynamic) = d % 2
