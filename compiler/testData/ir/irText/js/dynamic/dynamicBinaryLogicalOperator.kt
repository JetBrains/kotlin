// TARGET_BACKEND: JS_IR
// FIR_IDENTICAL

// NO_SIGNATURE_DUMP
// ^ KT-57566

fun testAndAnd(d: dynamic) = d && d
fun testOrOr(d: dynamic) = d || d
