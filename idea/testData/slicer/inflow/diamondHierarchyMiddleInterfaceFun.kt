// FLOW: IN

interface A {
    fun foo() = 1
}

open class B : A {
    override fun foo() = 2
}

interface C : A {
    override fun foo() = 3
}

class D : B(), C {
    override fun foo() = 4
}

fun test(a: A, b: B, c: C, d: D) {
    val x = a.foo()
    val y = b.foo()
    val <caret>z = c.foo()
    val u = d.foo()
}