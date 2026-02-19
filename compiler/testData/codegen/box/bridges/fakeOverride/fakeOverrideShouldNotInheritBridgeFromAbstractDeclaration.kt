interface IA {
    fun foo(): Any
}

open class B {
    open fun foo(): CharSequence = "FAIL"
}

open class C : B(), IA

interface ID {
    fun foo(): Any
}

open class D : C(), ID

open class E {
    open fun foo(): String = "OK"
}

class F : E(), IA, ID {
    override fun foo(): String = "OK"
}

fun box(): String {
    val ia: IA = F()
    val b: B = object : B() {
        override fun foo(): CharSequence = F().foo()
    }
    val c: C = object : C() {
        override fun foo(): CharSequence = F().foo()
    }
    val id: ID = F()
    val e: E = F()
    val f = F()

    if (ia.foo() != "OK") return "FAIL: IA"
    if (b.foo() != "OK") return "FAIL: B"
    if (c.foo() != "OK") return "FAIL: C"
    if (id.foo() != "OK") return "FAIL: ID"
    if (e.foo() != "OK") return "FAIL: E"
    if (f.foo() != "OK") return "FAIL: F"

    return "OK"
}