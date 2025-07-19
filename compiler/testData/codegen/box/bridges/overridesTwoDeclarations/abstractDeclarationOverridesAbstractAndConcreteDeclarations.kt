interface IA {
    fun foo(): Any
}

open class B {
    open fun foo(): CharSequence = "FAIL"
}

abstract class C : B(), IA {
    abstract override fun foo(): String
}

class D : C() {
    override fun foo(): String = "OK"
}
fun box(): String {
    val a: IA = D()
    val b: B = D()
    val c: C = D()
    val d = D()

    if (a.foo() != "OK") return "FAIL: IA"
    if (b.foo() != "OK") return "FAIL: B"
    if (c.foo() != "OK") return "FAIL: C"
    if (d.foo() != "OK") return "FAIL: D"

    return "OK"
}
