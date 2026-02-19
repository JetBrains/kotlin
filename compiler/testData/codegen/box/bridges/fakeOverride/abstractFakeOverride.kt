interface IA {
    fun foo(): Any
}

interface IB {
    fun foo(): CharSequence
}

interface IC : IA, IB

class D : IC {
    override fun foo(): String = "OK"
}

fun box(): String {
    val a: IA = D()
    val b: IB = D()
    val c: IC = D()
    val d = D()

    if (a.foo() != "OK") return "FAIL: IA"
    if (b.foo() != "OK") return "FAIL: IB"
    if (c.foo() != "OK") return "FAIL: IC"
    if (d.foo() != "OK") return "FAIL: D"

    return "OK"
}