interface A {
    fun foo()
}

abstract class B : A {
    override fun foo() {}
}

interface C : A {}

fun main(c: C) {
    if (c !is B) return
    c.foo()
}
