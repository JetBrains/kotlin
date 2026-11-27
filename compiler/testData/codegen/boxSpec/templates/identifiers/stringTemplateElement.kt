fun box(): String? {
    val <!ELEMENT(1)!> = "0"
    val <!ELEMENT(2)!> = "1"

    val x1 = "${<!ELEMENT(2)!>}"
    val x2 = "..." + "...$<!ELEMENT(1)!>..." + "..."
    var x3 = "$<!ELEMENT(2)!>${<!ELEMENT(1)!>}$<!ELEMENT(2)!>"

    if (<!ELEMENT(2)!> != "1") return null
    if (<!ELEMENT(1)!> != "0") return null

    if (x1 != "1") return null
    if (x2 != "......0......") return null
    if (x3 != "101") return null

    return "OK"
}
