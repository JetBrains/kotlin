// TARGET_BACKEND: JS_IR

// NO_SIGNATURE_DUMP
// ^ KT-57566

// FIR_IDENTICAL
fun test1(d: dynamic) = d.member

fun test2(d: dynamic) = d?.member
