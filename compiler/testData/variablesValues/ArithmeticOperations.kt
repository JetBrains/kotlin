fun operations() {
    val a = 0 + 1
    val b = a + 2
    val c = b - a
    var d = c * b
    d = (d + 12 / 5) * 11
    d = d * 2
}

fun operationsWithUnknown(a: Int, b: Int) {
    var c = a + 1
    c = a / b
}