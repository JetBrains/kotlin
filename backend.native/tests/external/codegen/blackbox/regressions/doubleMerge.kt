fun foo(): Double {
    var d = 2.0
    if (d > 0.0) {
        d++
    }
    d++
    return d
}

fun box() = if (foo() > 3.5) "OK" else "Fail"
