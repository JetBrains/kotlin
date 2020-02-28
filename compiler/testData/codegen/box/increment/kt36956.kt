class Cell(var x: Int) {
    operator fun get(i: Int) = x
    operator fun set(i: Int, v: Int) { x = v }
}

fun box(): String {
    val c = Cell(0)
    (c[0])++
    if (c[0] != 1) return "Fail"
    return "OK"
}