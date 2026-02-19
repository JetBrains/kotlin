interface IA {
    fun foo(): Any
}

interface IB : IA {
    override fun foo(): CharSequence
}

open class C : IA {
    override fun foo(): String = "OK"
}

class D : IB, C()

fun box(): String {
    val a: IA = D()
    val b: IB = D()
    val c: C = D()
    val d = D()

    if (a.foo() != "OK") return "FAIL: IA"
    if (b.foo() != "OK") return "FAIL: IB"
    if (c.foo() != "OK") return "FAIL: C"
    if (d.foo() != "OK") return "FAIL: D"

    return "OK"
}