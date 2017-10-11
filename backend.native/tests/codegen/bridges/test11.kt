package codegen.bridges.test11

import kotlin.test.*

interface I {
    fun foo(x: Int)
}

abstract class A<T> {
    abstract fun foo(x: T)
}

class B : A<Int>(), I {
    override fun foo(x: Int) = println(x)
}

@Test fun runTest() {
    val b = B()
    val a: A<Int> = b
    val c: I = b
    b.foo(42)
    a.foo(42)
    c.foo(42)
}