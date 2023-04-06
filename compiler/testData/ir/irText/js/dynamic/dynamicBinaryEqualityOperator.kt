// TARGET_BACKEND: JS_IR
// FIR_IDENTICAL

// MUTE_SIGNATURE_COMPARISON_K2: ANY
// ^ KT-57566

fun testEqeq(d: dynamic) = d == 3
fun testExclEq(d: dynamic) = d != 3
fun testEqeqeq(d: dynamic) = d === 3
fun testExclEqeq(d: dynamic) = d !== 3
