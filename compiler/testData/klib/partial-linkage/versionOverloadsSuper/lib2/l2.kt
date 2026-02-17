fun computeSuper(): String {

    val a1 = A()
    val a2 = A(2)

    val b1 = B()
    val b2 = B(2)
    val b3 = B(true)

    val c1 = C()
    val c2 = C(2)
    val c3 = C(true)

    if (a1.foo() != "A1") return "FAIL1"
    if (a2.foo() != "A1") return "FAIL2"

    if (b1.foo() != "B2") return "FAIL3"
    if (b2.foo() != "B1") return "FAIL4"
    if (b3.foo() != "A1") return "FAIL5"

    if (c1.foo() != "C2") return "FAIL6"
    if (c2.foo() != "C1") return "FAIL7"
    if (c3.foo() != "C1") return "FAIL8"

    return "OK"
}