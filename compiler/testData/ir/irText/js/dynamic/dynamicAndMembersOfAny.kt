// TARGET_BACKEND: JS_IR

// FIR_IDENTICAL
// WITH_STDLIB

fun test1(d: dynamic) = d.toString()

fun test2(d: dynamic) = d.hashCode()

fun test3(d: dynamic) = d.equals(42)
