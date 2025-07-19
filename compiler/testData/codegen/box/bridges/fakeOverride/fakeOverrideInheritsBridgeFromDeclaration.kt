open class A {
    open fun foo(): Any = "FAIL"
}

interface IB {
    fun foo(): CharSequence
}

open class C : A(), IB{
    override fun foo(): String = "OK"
}

open class D : C() {
    override fun foo(): String = "OK"
}

interface IE {
    fun foo(): String
}

class F : D(), IE

fun box(): String {
    val a: A = F()
    val b: IB = F()
    val c: C = F()
    val d: D = F()
    val e: IE = F()
    val f = F()

    if (a.foo() != "OK") return "FAIL: A"
    if (b.foo() != "OK") return "FAIL: IB"
    if (c.foo() != "OK") return "FAIL: C"
    if (d.foo() != "OK") return "FAIL: D"
    if (e.foo() != "OK") return "FAIL: IE"
    if (f.foo() != "OK") return "FAIL: F"

    return "OK"
}