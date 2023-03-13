// IGNORE_BACKEND_K2: JS_IR
// IGNORE_BACKEND_K2: JS_IR_ES6

fun test(f: () -> Unit) =
        try { f() } catch (e: Exception) { throw e }
