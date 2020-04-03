// IGNORE_BACKEND: JVM_IR, JS, JS_IR, NATIVE
// IGNORE_BACKEND_FIR: JVM_IR
// FILE: test.kt

fun checkEqual(x: Any, y: Any) {
    if (x != y || y != x) throw AssertionError("$x and $y should be equal")
    if (x.hashCode() != y.hashCode()) throw AssertionError("$x and $y should have the same hash code")
}

fun checkNotEqual(x: Any, y: Any) {
    if (x == y || y == x) throw AssertionError("$x and $y should NOT be equal")
}

fun target(x: Int, y: String = "", z: String = ""): Int = x

fun captureNoDefaults(fn: (Int, String, String) -> Unit): Any = fn
fun captureOneDefault(fn: (Int, String) -> Unit): Any = fn
fun captureAllDefaults(fn: (Int) -> Unit): Any = fn

fun box(): String {
    checkEqual(captureNoDefaults(::target), captureNoDefaults(::target))
    checkEqual(captureOneDefault(::target), captureOneDefault(::target))
    checkEqual(captureAllDefaults(::target), captureAllDefaults(::target))
    checkEqual(captureNoDefaults(::target), captureNoDefaultsFromOtherFile())

    checkNotEqual(captureNoDefaults(::target), captureOneDefault(::target))
    checkNotEqual(captureNoDefaults(::target), captureAllDefaults(::target))

    return "OK"
}

// FILE: fromOtherFile.kt

fun captureNoDefaultsFromOtherFile(): Any = captureNoDefaults(::target)
