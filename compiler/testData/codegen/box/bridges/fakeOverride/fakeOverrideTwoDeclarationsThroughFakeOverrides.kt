open class A {
    open fun foo(): CharSequence = "OK"
}

open class B : A()

interface IC {
    fun foo(): String
}

interface ID : IC

class E : B(), ID {
    override fun foo(): String = "OK"
}

fun box(): String {
    val a: A = E()
    val b: B = E()
    val c: IC = E()
    val d: ID = E()
    val e = E()

    if (a.foo() != "OK") return "FAIL: A"
    if (b.foo() != "OK") return "FAIL: B"
    if (c.foo() != "OK") return "FAIL: IC"
    if (d.foo() != "OK") return "FAIL: ID"
    if (e.foo() != "OK") return "FAIL: E"

    return "OK"
}