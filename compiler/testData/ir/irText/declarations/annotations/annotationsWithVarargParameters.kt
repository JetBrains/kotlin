// FIR_IDENTICAL

// IGNORE_BACKEND_K2: JS_IR
// IGNORE_BACKEND_K2: JS_IR_ES6

annotation class A(vararg val xs: String)

@A("abc", "def") fun test1() {}
@A("abc") fun test2() {}
@A fun test3() {}
