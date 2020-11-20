// FILE: test.kt

fun checkNotEqual(x: Any, y: Any) {
    if (x == y || y == x) throw AssertionError("$x and $y should NOT be equal")
}

fun target(s1: String, vararg xs: Int, s2: String = "") {}

fun capture1(fn: (String, IntArray, String) -> Unit): Any = fn
fun capture2(fn: (String, Int, Int) -> Unit): Any = fn

fun box(): String {
    checkNotEqual(capture1(::target), capture2(::target))
    checkNotEqual(capture1(::target), captureFromOtherFile())
    return "OK"
}

// FILE: fromOtherFile.kt

fun captureFromOtherFile(): Any = capture2(::target)
