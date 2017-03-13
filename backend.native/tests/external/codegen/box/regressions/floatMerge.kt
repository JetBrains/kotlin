fun foo(): Float {
    var f = 2.0f
    if (f > 0.0f) {
        f++
    }
    f++
    return f
}

fun box() = if (foo() > 3.5f) "OK" else "Fail"