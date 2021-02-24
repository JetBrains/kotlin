// DONT_TARGET_EXACT_BACKEND: WASM
// WASM_MUTE_REASON: FAILS_IN_JS_IR
// IGNORE_BACKEND: JS, JS_IR
// IGNORE_BACKEND: JS_IR_ES6
// FILE: test.kt

fun checkEqual(x: Any, y: Any) {
    if (x != y || y != x) throw AssertionError("$x and $y should be equal")
    if (x.hashCode() != y.hashCode()) throw AssertionError("$x and $y should have the same hash code")
}

fun checkNotEqual(x: Any, y: Any) {
    if (x == y || y == x) throw AssertionError("$x and $y should NOT be equal")
}

class V {
    fun target(vararg x: String) {}
}

private fun captureVararg2(f: (V, String, String) -> Unit): Any = f
private fun captureVararg1(f: (V, String) -> Unit): Any = f
private fun captureVararg0(f: (V) -> Unit): Any = f
private fun captureVarargAsArray(f: (V, Array<String>) -> Unit): Any = f

private fun captureVararg2Bound(f: (String, String) -> Unit): Any = f
private fun captureVararg1Bound(f: (String) -> Unit): Any = f
private fun captureVararg0Bound(f: () -> Unit): Any = f
private fun captureVarargAsArrayBound(f: (Array<String>) -> Unit): Any = f

fun box(): String {
    val v0 = V()

    checkEqual(captureVararg2(V::target), captureVararg2(V::target))
    checkEqual(captureVararg1(V::target), captureVararg1(V::target))
    checkEqual(captureVararg0(V::target), captureVararg0(V::target))
    checkEqual(captureVararg0(V::target), captureVararg0FromOtherFile())
    checkEqual(captureVarargAsArray(V::target), captureVarargAsArrayFromOtherFile())

    checkEqual(captureVararg2Bound(v0::target), captureVararg2Bound(v0::target))
    checkEqual(captureVararg1Bound(v0::target), captureVararg1Bound(v0::target))
    checkEqual(captureVararg0Bound(v0::target), captureVararg0Bound(v0::target))
    checkEqual(captureVararg0Bound(v0::target), captureVararg0BoundFromOtherFile(v0))
    checkEqual(captureVarargAsArrayBound(v0::target), captureVarargAsArrayBoundFromOtherFile(v0))


    checkNotEqual(captureVararg2(V::target), captureVararg0(V::target))
    checkNotEqual(captureVararg2Bound(v0::target), captureVararg0Bound(v0::target))

    checkNotEqual(captureVararg2(V::target), captureVarargAsArray(V::target))
    checkNotEqual(captureVararg1(V::target), captureVarargAsArray(V::target))
    checkNotEqual(captureVararg0(V::target), captureVarargAsArray(V::target))
    checkNotEqual(captureVararg1Bound(v0::target), captureVarargAsArrayBound(v0::target))
    checkNotEqual(captureVararg1Bound(v0::target), captureVarargAsArrayBoundFromOtherFile(v0))

    val v1 = V()
    checkNotEqual(captureVararg0Bound(v0::target), captureVararg0Bound(v1::target))
    checkNotEqual(captureVarargAsArrayBound(v0::target), captureVarargAsArrayBound(v1::target))

    return "OK"
}

// FILE: fromOtherFile.kt

private fun captureVararg0(f: (V) -> Unit): Any = f
private fun captureVararg0Bound(f: () -> Unit): Any = f
private fun captureVarargAsArray(f: (V, Array<String>) -> Unit): Any = f
private fun captureVarargAsArrayBound(f: (Array<String>) -> Unit): Any = f

fun captureVararg0FromOtherFile(): Any = captureVararg0(V::target)
fun captureVararg0BoundFromOtherFile(v0: V): Any = captureVararg0Bound(v0::target)
fun captureVarargAsArrayFromOtherFile(): Any = captureVarargAsArray(V::target)
fun captureVarargAsArrayBoundFromOtherFile(v0: V): Any = captureVarargAsArrayBound(v0::target)
