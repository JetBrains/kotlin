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
    fun target(x: String = "x", y: String = "y", z: String = "z"): String = x + y + z
}

private fun captureNoDefaults(f: (V, String, String, String) -> String): Any = f
private fun captureOneDefault(f: (V, String, String) -> String): Any = f
private fun captureAllDefaults(f: (V) -> String): Any = f

private fun captureNoDefaultsBound(f: (String, String, String) -> String): Any = f
private fun captureOneDefaultBound(f: (String, String) -> String): Any = f
private fun captureAllDefaultsBound(f: () -> String): Any = f

fun box(): String {
    val v0 = V()

    checkEqual(captureNoDefaults(V::target), captureNoDefaults(V::target))
    checkEqual(captureNoDefaults(V::target), captureNoDefaultsFromOtherFile())

    checkEqual(captureOneDefault(V::target), captureOneDefault(V::target))
    checkEqual(captureOneDefault(V::target), captureOneDefaultFromOtherFile())

    checkEqual(captureAllDefaults(V::target), captureAllDefaults(V::target))

    checkEqual(captureNoDefaultsBound(v0::target), captureNoDefaultsBound(v0::target))
    checkEqual(captureNoDefaultsBound(v0::target), captureNoDefaultsBoundFromOtherFile(v0))

    checkEqual(captureOneDefaultBound(v0::target), captureOneDefaultBound(v0::target))

    checkEqual(captureAllDefaultsBound(v0::target), captureAllDefaultsBound(v0::target))


    checkNotEqual(captureNoDefaults(V::target), captureOneDefault(V::target))
    checkNotEqual(captureNoDefaults(V::target), captureAllDefaults(V::target))

    checkNotEqual(captureNoDefaultsBound(v0::target), captureOneDefaultBound(v0::target))
    checkNotEqual(captureNoDefaultsBound(v0::target), captureAllDefaultsBound(v0::target))

    checkNotEqual(captureNoDefaults(V::target), captureNoDefaultsBoundFromOtherFile(v0))

    val v1 = V()
    checkNotEqual(captureNoDefaultsBound(v0::target), captureNoDefaultsBound(v1::target))
    checkNotEqual(captureOneDefaultBound(v0::target), captureOneDefaultBound(v1::target))
    checkNotEqual(captureAllDefaultsBound(v0::target), captureAllDefaultsBound(v1::target))

    return "OK"
}

// FILE: fromOtherFile.kt

private fun captureNoDefaults(f: (V, String, String, String) -> String): Any = f
private fun captureOneDefault(f: (V, String, String) -> String): Any = f
private fun captureNoDefaultsBound(f: (String, String, String) -> String): Any = f

fun captureNoDefaultsFromOtherFile(): Any = captureNoDefaults(V::target)
fun captureOneDefaultFromOtherFile(): Any = captureOneDefault(V::target)
fun captureNoDefaultsBoundFromOtherFile(v0: V) = captureNoDefaultsBound(v0::target)
