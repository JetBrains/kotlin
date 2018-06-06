// FLOW: OUT

class D : B(), C {
    override fun foo() = <caret>4
}

fun test(a: A, b: B, c: C, d: D) {
    val x = a.foo()
    val y = b.foo()
    val z = c.foo()
    val u = d.foo()
}