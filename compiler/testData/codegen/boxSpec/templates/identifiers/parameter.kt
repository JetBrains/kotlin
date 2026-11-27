<!DIRECTIVES("HELPERS: REFLECT")!>

fun f1(<!ELEMENT(1)!>: Boolean) = !!!<!ELEMENT(1)!>
fun f2(<!ELEMENT(2)!>: Boolean, <!ELEMENT(3)!>: Boolean) = <!ELEMENT(2)!> && <!ELEMENT(3)!>
fun f3(<!ELEMENT(4)!>: Boolean, <!ELEMENT(5)!>: Boolean = <!ELEMENT(4)!>) = <!ELEMENT(4)!> || !<!ELEMENT(5)!>

class A {
    var x1: Boolean = false
        set(<!ELEMENT(6)!>) {
            field = !<!ELEMENT(6)!>
        }
}

fun box(): String? {
    val a = A()
    a.x1 = false

    if (f1(true)) return null
    if (!f2(true, true)) return null
    if (f3(false, true)) return null
    if (!a.x1) return null

    if (!checkParameter(::f1, "<!ELEMENT_VALIDATION(1)!>")) return null
    if (!checkParameters(::f2, listOf("<!ELEMENT_VALIDATION(2)!>", "<!ELEMENT_VALIDATION(3)!>"))) return null
    if (!checkParameters(::f3, listOf("<!ELEMENT_VALIDATION(4)!>", "<!ELEMENT_VALIDATION(5)!>"))) return null
    if (!checkSetterParameterName(A::x1, "<!ELEMENT_VALIDATION(6)!>")) return null

    return "OK"
}
