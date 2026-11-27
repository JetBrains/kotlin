<!DIRECTIVES("WITH_STDLIB")!>

fun f1(<!ELEMENT(1)!>: Boolean, <!ELEMENT(2)!>: Boolean) = <!ELEMENT(1)!> && !!!<!ELEMENT(2)!>

fun f2(<!ELEMENT(3)!>: Boolean): Boolean {
    return !<!ELEMENT(3)!>
}

fun f3(vararg <!ELEMENT(4)!>: Boolean, <!ELEMENT(5)!>: Boolean) = <!ELEMENT(4)!>.any { it } && <!ELEMENT(5)!>

fun box(): String? {
    if (f1(<!ELEMENT(1)!> = false, <!ELEMENT(2)!> = true)) return null
    if (!f2(<!ELEMENT(3)!> = false && true || true && false)) return null
    if (!f3(<!ELEMENT(4)!> = *booleanArrayOf(true, false, false, true), <!ELEMENT(5)!> = true)) return null

    return "OK"
}
