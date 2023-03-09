// FIR_IDENTICAL

// IGNORE_BACKEND_K2: JS_IR
// IGNORE_BACKEND_K2: JS_IR_ES6

annotation class Test1(val x: Int)

annotation class Test2(val x: Int = 0)

annotation class Test3(val x: Test1)

annotation class Test4(vararg val xs: Int)
