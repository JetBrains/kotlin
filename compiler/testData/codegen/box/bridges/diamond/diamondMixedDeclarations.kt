interface IA {
    fun foo(): Any
}

open class B : IA {
    override fun foo(): CharSequence = "FAIL"
}

interface IC : IA

class D : B(), IC {
    override fun foo(): String = "OK"
}

fun box(): String {
    val a: IA = D()
    val b: B = D()
    val c: IC = D()
    val d = D()

    if (a.foo() != "OK") return "FAIL: IA"
    if (b.foo() != "OK") return "FAIL: B"
    if (c.foo() != "OK") return "FAIL: IC"
    if (d.foo() != "OK") return "FAIL: D"

    return "OK"
}