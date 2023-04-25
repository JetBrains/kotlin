// TARGET_BACKEND: JS_IR

// FIR_IDENTICAL
fun test1(d: dynamic) = d foo 123

fun test2(d: dynamic) = d invoke 123
