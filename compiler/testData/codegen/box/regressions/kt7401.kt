fun foo(): Long {
    var n = 2L
    if (n > 0L) {
        n++
    }
    n++
    return n
}

fun box() = if (foo() == 4L) "OK" else "Fail"
