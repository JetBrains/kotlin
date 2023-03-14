// FIR_IDENTICAL
// IGNORE_BACKEND_K1: JS_IR
// IGNORE_BACKEND_K1: JS_IR_ES6

annotation class A1
annotation class A2
annotation class A3

@[A1, A2, A3] fun test() {}
