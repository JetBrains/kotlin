// FIR_IDENTICAL

// IGNORE_BACKEND_K2: JS_IR
// IGNORE_BACKEND_K2: JS_IR_ES6

fun test1(a: Boolean, b: Boolean) = a && b
fun test2(a: Boolean, b: Boolean) = a || b

fun test1x(a: Boolean, b: Boolean) = a and b
fun test2x(a: Boolean, b: Boolean) = a or b
