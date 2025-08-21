interface IA {
    fun foo(): Any
}

interface IC {
    fun foo(): String
}

interface ID {
    fun foo(): Comparable<*>
}

open class B {
    open fun foo(): CharSequence = "OK"
}

class E : B(), IA, IC, ID {
    override fun foo(): String = "OK"
}

fun box(): String {
    val a: IA = E()
    val b: B = E()
    val c: IC = E()
    val d: ID = E()
    val e = E()

    if (a.foo() != "OK") return "FAIL: IA"
    if (b.foo() != "OK") return "FAIL: B"
    if (c.foo() != "OK") return "FAIL: IC"
    if (d.foo() != "OK") return "FAIL: ID"
    if (e.foo() != "OK") return "FAIL: E"

    return "OK"
}