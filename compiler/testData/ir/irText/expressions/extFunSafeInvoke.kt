// FIR_IDENTICAL

// IGNORE_BACKEND_K2: JS_IR
// IGNORE_BACKEND_K2: JS_IR_ES6

fun test(receiver: Any?, fn: Any.(Int, String) -> Unit) =
        receiver?.fn(42, "Hello")
