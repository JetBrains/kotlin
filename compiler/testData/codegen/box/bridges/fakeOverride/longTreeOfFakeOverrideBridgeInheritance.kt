open class A {
    open fun foo(): CharSequence = "OK"
}

interface B {
    fun foo(): Any
}

interface C {
    fun foo(): Any
}

interface E {
    fun foo(): Any
}

interface F {
    fun foo(): Any
}

interface H {
    fun foo(): String
}

interface I {
    fun foo(): String
}

open class D : A(), B, C

open class G : D(), E, F

class J : G(), H, I {
    override fun foo(): String = "OK"
}

fun box(): String {
    val a: A = J()
    val b: B = J()
    val c: C = J()
    val e: E = J()
    val f: F = J()
    val h: H = J()
    val i: I = J()
    val j = J()

    if (a.foo() != "OK") return "FAIL: A"
    if (b.foo() != "OK") return "FAIL: B"
    if (c.foo() != "OK") return "FAIL: C"
    if (e.foo() != "OK") return "FAIL: E"
    if (f.foo() != "OK") return "FAIL: F"
    if (h.foo() != "OK") return "FAIL: H"
    if (i.foo() != "OK") return "FAIL: I"
    if (j.foo() != "OK") return "FAIL: J"

    return "OK"
}