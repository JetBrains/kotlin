open class A {
    open fun foo(): CharSequence = "OK"
}

interface IB {
    fun foo(): String
}

abstract class C : A(), IB {
    override abstract fun foo(): String
}

class D : C() {
    override fun foo(): String = "OK"
}

fun box(): String {
    val a: A = D()
    val b: IB = D()
    val c: C = D()
    val d = D()

    if (a.foo() != "OK") return "FAIL: A"
    if (b.foo() != "OK") return "FAIL: IB"
    if (c.foo() != "OK") return "FAIL: C"
    if (d.foo() != "OK") return "FAIL: D"

    return "OK"
}