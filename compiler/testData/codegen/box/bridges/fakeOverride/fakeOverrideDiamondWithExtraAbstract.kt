open class A {
    open fun foo(): Any = "FAIL"
}

open class C : A() {
    override fun foo(): CharSequence = "OK"
}

interface ID {
    fun foo(): String
}

class E : C(), ID {
    override fun foo(): String = "OK"
}

fun box(): String {
    val a: A = E()
    val c: C = E()
    val d: ID = E()
    val e = E()

    if (a.foo() != "OK") return "FAIL: A"
    if (c.foo() != "OK") return "FAIL: C"
    if (d.foo() != "OK") return "FAIL: ID"
    if (e.foo() != "OK") return "FAIL: E"

    return "OK"
}