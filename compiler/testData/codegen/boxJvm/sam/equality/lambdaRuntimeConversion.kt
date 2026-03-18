// TARGET_BACKEND: JVM
// FILE: test.kt

fun checkNotEqual(x: Any, y: Any) {
    if (x == y || y == x) throw AssertionError("$x and $y should NOT be equal")
}

private fun id(f: Runnable): Any = f

val lambda = {}

fun box(): String {
    // Since 1.0, SAM wrappers for Java do not implement equals/hashCode
    checkNotEqual(id(lambda), id(lambda))
    checkNotEqual(id(lambda), lambdaFromOtherFile())
    return "OK"
}

// FILE: fromOtherFile.kt

private fun id(f: Runnable): Any = f

fun lambdaFromOtherFile(): Any = id(lambda)
