fun checkNotEqual(x: Any, y: Any) {
    if (x == y || y == x) throw AssertionError("$x and $y should NOT be equal")
}

fun box(): String {
    checkNotEqual({ 1 }, { 1 })
    if ({ 1 } === { 1 }) throw AssertionError("Lambdas should not be identity-equal")

    return "OK"
}
