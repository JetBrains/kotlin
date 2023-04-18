// TARGET_BACKEND: JS_IR
// FIR_IDENTICAL

// MUTE_SIGNATURE_COMPARISON_K2: ANY
// ^ KT-57818

val d: dynamic = 1

val p: Int = d

fun test1(d: dynamic): Int = d

fun test2(d: dynamic): Any = d

fun test3(d: dynamic): Any? = d

fun test4(d: dynamic): String = d.member(1, 2, 3)
