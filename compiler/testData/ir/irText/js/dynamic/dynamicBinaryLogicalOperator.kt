// TARGET_BACKEND: JS_IR
// FIR_IDENTICAL

// MUTE_SIGNATURE_COMPARISON_K2: ANY
// ^ KT-57566

fun testAndAnd(d: dynamic) = d && d
fun testOrOr(d: dynamic) = d || d
