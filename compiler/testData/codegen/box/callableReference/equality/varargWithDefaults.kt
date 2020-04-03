// IGNORE_BACKEND: JS, JS_IR, NATIVE
// IGNORE_BACKEND_FIR: JVM_IR
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
