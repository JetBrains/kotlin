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

class V {
    fun target(): String = ""
}

private fun captureString(f: (V) -> String): Any = f
private fun captureUnit(f: (V) -> Unit): Any = f

private fun captureStringBound(f: () -> String): Any = f
private fun captureUnitBound(f: () -> Unit): Any = f

fun box(): String {
    val v0 = V()
    
    checkEqual(captureString(V::target), captureString(V::target))
    checkEqual(captureString(V::target), captureStringFromOtherFile())
    checkEqual(captureUnit(V::target), captureUnit(V::target))
    checkEqual(captureUnit(V::target), captureUnitFromOtherFile())

    checkEqual(captureStringBound(v0::target), captureStringBound(v0::target))
    checkEqual(captureStringBound(v0::target), captureStringBoundFromOtherFile(v0))
    checkEqual(captureUnitBound(v0::target), captureUnitBound(v0::target))
    checkEqual(captureUnitBound(v0::target), captureUnitBoundFromOtherFile(v0))


    checkNotEqual(captureString(V::target), captureUnit(V::target))
    checkNotEqual(captureStringBound(v0::target), captureUnitBound(v0::target))
    checkNotEqual(captureString(V::target), captureUnitBoundFromOtherFile(v0))

    val v1 = V()
    checkNotEqual(captureStringBound(v0::target), captureStringBound(v1::target))
    checkNotEqual(captureUnitBound(v0::target), captureUnitBound(v1::target))

    return "OK"
}

// FILE: fromOtherFile.kt

private fun captureString(f: (V) -> String): Any = f
private fun captureUnit(f: (V) -> Unit): Any = f

private fun captureStringBound(f: () -> String): Any = f
private fun captureUnitBound(f: () -> Unit): Any = f

fun captureStringFromOtherFile(): Any = captureString(V::target)
fun captureUnitFromOtherFile(): Any = captureUnit(V::target)
fun captureStringBoundFromOtherFile(v0: V): Any = captureStringBound(v0::target)
fun captureUnitBoundFromOtherFile(v0: V): Any = captureUnitBound(v0::target)
