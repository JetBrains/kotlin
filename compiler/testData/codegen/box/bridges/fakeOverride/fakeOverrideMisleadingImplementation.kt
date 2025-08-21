interface A {
    fun foo(): Any
}

interface B {
    fun foo(): CharSequence
}

interface C : A {
    override fun foo(): Any
}

open class D {
    open fun foo(): String = "OK"
}

open class E : D()

class F : E(), A, B, C

fun box(): String {
    val a: A = F()
    val b: B = F()
    val c: C = F()
    val d: D = F()
    val e: E = F()
    val f = F()

    if (a.foo() != "OK") return "FAIL: A"
    if (b.foo() != "OK") return "FAIL: B"
    if (c.foo() != "OK") return "FAIL: C"
    if (d.foo() != "OK") return "FAIL: D"
    if (e.foo() != "OK") return "FAIL: E"
    if (f.foo() != "OK") return "FAIL: F"

    return "OK"
}