// TARGET_BACKEND: JS_IR
// FIR_IDENTICAL

// MUTE_SIGNATURE_COMPARISON_K2: ANY
// ^ KT-57566

fun test1(d: dynamic) = d.member

fun test2(d: dynamic) = d?.member
