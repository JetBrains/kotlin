// FIR_IDENTICAL

// IGNORE_BACKEND_K2: JS_IR
// IGNORE_BACKEND_K2: JS_IR_ES6

fun test(x: Any) =
    x == (if (x !is Double) null!! else x)
