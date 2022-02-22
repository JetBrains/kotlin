// WITH_STDLIB

class Cell {
    operator fun get(s: Int) = 1
}

fun box(): String {
    val c = Cell()
    (<!UNRESOLVED_REFERENCE!>c[0]<!>)++
    return "OK"
}
