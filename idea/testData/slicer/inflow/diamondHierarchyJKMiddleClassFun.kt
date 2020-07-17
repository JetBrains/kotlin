// FLOW: IN

open class B : A {
    override fun foo() = 2
}

internal fun test(a: A, b: B, c: C, d: D) {
    val x = a.foo()
    val <caret>y = b.foo()
    val z = c.foo()
    val u = d.foo()
}