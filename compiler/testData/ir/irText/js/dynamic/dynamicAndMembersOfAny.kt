// TARGET_BACKEND: JS_IR
// FIR_IDENTICAL
// WITH_STDLIB

// MUTE_SIGNATURE_COMPARISON_K2: ANY
// ^ KT-57566

fun test1(d: dynamic) = d.toString()

fun test2(d: dynamic) = d.hashCode()

fun test3(d: dynamic) = d.equals(42)
