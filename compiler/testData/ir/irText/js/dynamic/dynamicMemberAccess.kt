// TARGET_BACKEND: JS_IR
// FIR_IDENTICAL
fun test1(d: dynamic) = d.member

fun test2(d: dynamic) = d?.member
