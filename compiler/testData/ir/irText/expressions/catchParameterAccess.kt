// IGNORE_BACKEND: JS_IR
// IGNORE_BACKEND: JS_IR_ES6

fun test(f: () -> Unit) =
        try { f() } catch (e: Exception) { throw e }
