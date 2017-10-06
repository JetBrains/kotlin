// PROBLEM: none
interface A

interface B {
    fun test()
}

interface Foo : A, B

open class Bar : Foo {
    override fun test() {}
    open fun test(a: String) {}
    open fun test2() {}
}

class Baz(val foo: Foo) : Bar(), Foo by foo {
    override <caret>fun test() = super.test()
}
