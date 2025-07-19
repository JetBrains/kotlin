interface IA {
    fun foo(): Any
}

open class B : IA {
    override fun foo(): CharSequence = "FAIL"
}

class C : B() {
    override fun foo(): String = "OK"
}

fun box(): String {
    val a: IA = C()
    val b: B = C()
    val c = C()

    if (a.foo() != "OK") return "FAIL: IA"
    if (b.foo() != "OK") return "FAIL: B"
    if (c.foo() != "OK") return "FAIL: C"

    return "OK"
}