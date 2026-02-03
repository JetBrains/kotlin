fun checkEqual(x: Any, y: Any) {
    if (x != y || y != x) throw AssertionError("$x and $y should be equal")
    if (x.hashCode() != y.hashCode()) throw AssertionError("$x and $y should have the same hash code")
}

fun checkNotEqual(x: Any, y: Any) {
    if (x == y || y == x) throw AssertionError("$x and $y should NOT be equal")
}

fun topLevelOverloaded(x: Int) = x
fun topLevelOverloaded(x: String) = x

fun captureInt(f: (Int) -> Int): Any = f
fun captureString(f: (String) -> String): Any = f

fun box(): String {
    checkNotEqual(captureInt(::topLevelOverloaded), captureString(::topLevelOverloaded))
    checkEqual(captureInt(::topLevelOverloaded), captureInt(::topLevelOverloaded))

    return "OK"
}
