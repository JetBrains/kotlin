// WITH_STDLIB

class Cell {
    operator fun get(s: Int) = 1
}

fun box(): String {
    val c = Cell()
    (<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>c[0]<!>)++
    return "OK"
}
