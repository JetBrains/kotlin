open class A {
    open fun foo(): Any = "FAIL"
}

open class B : A() {
    override fun foo(): CharSequence = "FAIL"
}

interface IC {
    fun foo(): String
}

class D : B(), IC {
    override fun foo(): String = "OK"
}

fun box(): String {
    val a: A = D()
    val b: B = D()
    val c: IC = D()
    val d = D()

    if (a.foo() != "OK") return "FAIL: A"
    if (b.foo() != "OK") return "FAIL: B"
    if (c.foo() != "OK") return "FAIL: IC"
    if (d.foo() != "OK") return "FAIL: D"

    return "OK"
}