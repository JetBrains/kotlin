infix fun Int.<!ELEMENT(1)!>(value: Int) = value > 100

infix fun Int.<!ELEMENT(2)!>(value: Int): Int {
    return value - 90
}

fun box(): String? {
    if (1 + 1 <!ELEMENT(1)!> -1001020) return null
    if (1 + 1 <!ELEMENT(2)!> 2004 <!ELEMENT(1)!> -0) return null

    return "OK"
}
