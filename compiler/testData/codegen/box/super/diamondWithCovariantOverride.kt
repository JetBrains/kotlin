// IGNORE_BACKEND_K1: ANY

interface Foo<T> {
    fun foo(): T
}

interface Foo2 : Foo<String> {
    override fun foo(): String = "OK"
}

abstract class A1<T> : Foo<T>

open class A2 : A1<String>(), Foo2

open class A3 : A2() {
    fun test(): String = super.foo()
}

class A4 : A3() {
    override fun foo(): String = "Fail"
}

fun box(): String = A4().test()
