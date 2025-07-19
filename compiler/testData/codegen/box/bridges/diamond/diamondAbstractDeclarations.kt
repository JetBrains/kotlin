interface IA {
    fun foo(): Any
}

interface IB : IA

interface IC : IA

interface ID : IB, IC

class E : ID {
    override fun foo(): String = "OK"
}

fun box(): String {
    val a: IA = E()
    val b: IB = E()
    val c: IC = E()
    val d: ID = E()
    val e = E()

    if (a.foo() != "OK") return "FAIL: IA"
    if (b.foo() != "OK") return "FAIL: IB"
    if (c.foo() != "OK") return "FAIL: IC"
    if (d.foo() != "OK") return "FAIL: ID"
    if (e.foo() != "OK") return "FAIL: E"

    return "OK"
}