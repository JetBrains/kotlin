// WASM_MUTE_REASON: FAILS_IN_JS_IR
// IGNORE_BACKEND: JS_IR
// IGNORE_BACKEND: JS_IR_ES6
// FILE: test.kt

fun checkEqual(x: Any, y: Any) {
    if (x != y || y != x) throw AssertionError("$x and $y should be equal")
    if (x.hashCode() != y.hashCode()) throw AssertionError("$x and $y should have the same hash code")
}

fun target(x: Int = 0, vararg ys: String) {}

fun captureAll1(fn: () -> Unit): Any = fn
fun captureAll2(fn: () -> Unit): Any = fn

fun box(): String {
    checkEqual(captureAll1(::target), captureAll2(::target))
    return "OK"
}

// FILE: fromOtherFile.kt

fun captureAllFromOtherFile(): Any = captureAll1(::target)
