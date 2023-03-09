// FIR_IDENTICAL

// IGNORE_BACKEND_K2: JS_IR
// IGNORE_BACKEND_K2: JS_IR_ES6

fun test1(a: String, b: Any) = a + b
fun test2(a: String, b: Int) = a + "+" + b
fun test3(a: String, b: Int) = (a + "+") + (b + 1) + a
