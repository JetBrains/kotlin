// IGNORE_BACKEND: JS_IR
// IGNORE_BACKEND: JS_IR_ES6
// FILE: test.kt

fun checkEqual(x: Any, y: Any) {
    if (x != y || y != x) throw AssertionError("$x and $y should be equal")
    if (x.hashCode() != y.hashCode()) throw AssertionError("$x and $y should have the same hash code")
}

fun checkNotEqual(x: Any, y: Any) {
    if (x == y || y == x) throw AssertionError("$x and $y should NOT be equal")
}

fun target(x: Int, vararg ys: String): Int = x + ys.size

fun captureVararg1(fn: (Int, String) -> Unit): Any = fn
fun captureVararg0(fn: (Int) -> Unit): Any = fn

fun captureNoVararg(fn: (Int, Array<String>) -> Int): Any = fn
fun captureNoVarargCoerced(fn: (Int, Array<String>) -> Unit): Any = fn

fun box(): String {
    checkEqual(captureVararg1(::target), captureVararg1(::target))
    checkEqual(captureVararg0(::target), captureVararg0(::target))
    checkEqual(captureVararg1(::target), captureVararg1FromOtherFile())

    checkNotEqual(captureVararg1(::target), captureVararg0(::target))

    checkNotEqual(captureNoVararg(::target), captureNoVarargCoerced(::target))

    return "OK"
}

// FILE: fromOtherFile.kt

fun captureVararg1FromOtherFile(): Any = captureVararg1(::target)
