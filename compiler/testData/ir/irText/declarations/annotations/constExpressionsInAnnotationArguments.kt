// FIR_IDENTICAL

// MUTE_SIGNATURE_COMPARISON_K2: JS_IR
// MUTE_SIGNATURE_COMPARISON_K2: NATIVE
// ^ KT-57818

const val ONE = 1

annotation class A(val x: Int)

@A(ONE) fun test1() {}
@A(1+1) fun test2() {}
