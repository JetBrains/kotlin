// FLOW: IN

interface A {
    fun foo() = 1
}

internal fun test(a: A, b: B, c: C, d: D) {
    val <caret>x = a.foo()
    val y = b.foo()
    val z = c.foo()
    val u = d.foo()
}