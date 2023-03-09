// TARGET_BACKEND: JS_IR

// NO_SIGNATURE_DUMP
// ^ KT-57566

// FIR_IDENTICAL
fun test1(d: dynamic) = d foo 123

fun test2(d: dynamic) = d invoke 123
