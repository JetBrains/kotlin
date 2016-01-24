// KT-9682 Overload Resolution Ambiguity after casting to Interface

open class Foo {
    fun bar(): Foo {
        return this
    }
}

class Foo2 : Foo(), IFoo

interface IFoo {
    fun bar(): Foo
}

fun test() {
    val foo : Foo = Foo2()
    foo as IFoo
    foo.bar() // Should be resolved to Foo#bar
}

interface IFoo2 {
    fun bar(): IFoo2
}

fun test2(foo: Foo) {
    foo as IFoo2
    foo.<!OVERLOAD_RESOLUTION_AMBIGUITY!>bar<!>() // should be ambiguity
}