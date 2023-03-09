// FIR_IDENTICAL

// IGNORE_BACKEND_K2: JS_IR
// IGNORE_BACKEND_K2: JS_IR_ES6

const val ONE = 1

annotation class A(val x: Int)

@A(ONE) fun test1() {}
@A(1+1) fun test2() {}
