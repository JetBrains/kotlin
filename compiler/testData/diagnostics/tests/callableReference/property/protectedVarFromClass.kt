// KT-12982 Incorrect type inference when accessing mutable protected property via reflection

import kotlin.reflect.KMutableProperty1

class Foo {
    protected var x = 0

    fun baz(p: KMutableProperty1<Foo, Int>) = p
    fun print() = baz(Foo::x)
}


open class A {
    protected fun a() {}
}

open class B : A() {
    val x = C::a
    val y = C()::a
}

class C : B()
