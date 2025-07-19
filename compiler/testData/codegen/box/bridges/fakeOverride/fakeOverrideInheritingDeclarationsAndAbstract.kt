interface IA {
    fun foo(): Any
}

interface IB {
    fun foo(): CharSequence
}

open class C : IB {
    override fun foo(): String = "OK"
}

class E : C(), IA, IB

fun box(): String {
    val a: IA = E()
    val b: IB = E()
    val c: C = E()
    val e = E()

    if (a.foo() != "OK") return "FAIL: IA"
    if (b.foo() != "OK") return "FAIL: IB"
    if (c.foo() != "OK") return "FAIL: C"
    if (e.foo() != "OK") return "FAIL: E"

    return "OK"
}