interface IA {
    fun foo(): Any
}

interface IB {
    fun foo(): CharSequence
}

class C : IA, IB {
    override fun foo(): String = "OK"
}

fun box(): String {
    val a: IA = C()
    val b: IB = C()
    val c = C()

    if (a.foo() != "OK") return "FAIL: IA"
    if (b.foo() != "OK") return "FAIL: IB"
    if (c.foo() != "OK") return "FAIL: C"

    return "OK"
}