<!DIRECTIVES("HELPERS: REFLECT")!>

class A {
    var x1: String = "100"
        set(<!ELEMENT(1)!>) {
            field = "$<!ELEMENT(1)!> 10"
        }
}

object B {
    var x2: String = "101"
        set(<!ELEMENT(2)!>) = kotlin.Unit
}

var x3: String = "102"
    set(<!ELEMENT(3)!>) {
        field = "${<!ELEMENT(3)!>} 11"
    }

fun box(): String? {
    val a = A()
    a.x1 = "0"
    B.x2 = "1"
    x3 = "2"

    if (a.x1 != "0 10") return null
    if (B.x2 != "101") return null
    if (x3 != "2 11") return null

    if (!checkSetterParameterName(A::x1, "<!ELEMENT_VALIDATION(1)!>")) return null
    if (!checkSetterParameterName(B::x2, "<!ELEMENT_VALIDATION(2)!>")) return null
    if (!checkSetterParameterName(::x3, "<!ELEMENT_VALIDATION(3)!>")) return null

    return "OK"
}
