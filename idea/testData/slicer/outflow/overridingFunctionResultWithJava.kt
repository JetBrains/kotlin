// FLOW: OUT

internal open class B : A() {
    override fun foo() = <caret>2
}

internal fun test(a: A, b: B, c: C) {
    val x = a.foo()
    val y = b.foo()
    val z = c.foo()
}