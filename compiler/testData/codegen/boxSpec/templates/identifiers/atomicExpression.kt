fun box(): String? {
    val <!ELEMENT(1)!> = 10
    val <!ELEMENT(2)!> = "."

    val value_1 = <!ELEMENT(1)!> - 100 % <!ELEMENT(1)!>
    val value_2 = <!ELEMENT(1)!>.dec()
    val value_3 = "$<!ELEMENT(2)!> 10"
    val value_4 = "${<!ELEMENT(2)!>}"
    val value_5 = <!ELEMENT(2)!> + " 11..." + <!ELEMENT(2)!> + "1"
    val value_6 = <!ELEMENT(1)!>

    if (value_1 != 10) return null
    if (value_2 != 9) return null
    if (value_3 != ". 10") return null
    if (value_4 != ".") return null
    if (value_5 != ". 11....1") return null
    if (value_6 != 10) return null

    return "OK"
}