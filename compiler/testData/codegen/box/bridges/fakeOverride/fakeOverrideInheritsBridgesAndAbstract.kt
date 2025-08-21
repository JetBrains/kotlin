interface IA {
    fun foo(): Any
}

interface ID {
    fun foo(): String
}

open class B {
    open fun foo(): CharSequence = "OK"
}

open class C : B(), IA

class E : C(), ID {
    override fun foo(): String = "OK"
}

fun box(): String {
    val a: IA = E()
    val b: B = E()
    val c: C = E()
    val d: ID = E()
    val e = E()

    if (a.foo() != "OK") return "FAIL: IA"
    if (b.foo() != "OK") return "FAIL: B"
    if (c.foo() != "OK") return "FAIL: C"
    if (d.foo() != "OK") return "FAIL: ID"
    if (e.foo() != "OK") return "FAIL: E"

    return "OK"
}