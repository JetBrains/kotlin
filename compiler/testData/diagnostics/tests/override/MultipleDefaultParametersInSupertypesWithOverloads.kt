// FIR_IDENTICAL

interface Foo<T> {
    fun foo() {}
    fun foo(param: Int = 1) {}
}

open class Test<K> : Foo<K> {
    override fun foo() {}
}

open class Rest<R> : Test<R>(), Foo<R>

class Baz : Rest<Int>() {}
