// PROBLEM: none
interface Foo {
    fun test(a: String, b: Int)
}

open class Bar : Foo {
    override fun test(a: String, b: Int) {}
    open fun test(a: String) {}
    open fun test2() {}
}

class Baz(val foo: Foo) : Bar(), Foo by foo {
    override <caret>fun test(a: String, b: Int) = super.test(a, b)
}
