// IGNORE_BACKEND: JS, JS_IR, JS_IR_ES6, WASM
var result = ""

fun sideEffecting(): Int {
    result += "OK"
    return 123
}

class C(val x: Int)

val a: C? = C(123)
val b: C? = null

fun box(): String {
    if (a?.x != sideEffecting()) return "fail cmp 1"
    // RHS not evaluated because `b` is null, might be a bug:
    if (b?.x == sideEffecting()) return "fail cmp 2"
    return result
}
