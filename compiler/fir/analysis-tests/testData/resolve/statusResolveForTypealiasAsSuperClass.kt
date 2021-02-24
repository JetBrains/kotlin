class A : Foo {
    override fun foo() {}
}

typealias Foo = B

interface B {
    fun foo() {}
}

fun test(c: A) {
    c.foo()
}
