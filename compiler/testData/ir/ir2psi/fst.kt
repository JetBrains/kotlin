fun foo(x: Int): Int {
    var y = x + 2
    if (y > 2) {
        return 10
    } else {
        y += 10
    }
    return y
}

fun bar(): Int {
    val x = 10
    val y = foo(x)
    return y + 10
}