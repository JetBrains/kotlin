fun Foo() {}

fun FOO_BAR() {}

fun xyzzy() {}

fun `a b`() {}

interface I {
    fun a_b()
}

class C : I {
    override fun a_b() {} // Shouldn't be reported
}