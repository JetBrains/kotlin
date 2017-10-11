package codegen.bridges.test12

import kotlin.test.*

abstract class A<in T> {
    abstract fun foo(x: T)
}

class B : A<Int>() {
    override fun foo(x: Int) {
        println("B: $x")
    }
}

class C : A<Any>() {
    override fun foo(x: Any) {
        println("C: $x")
    }
}

fun foo(arg: A<Int>) {
    arg.foo(42)
}

@Test fun runTest() {
    foo(B())
    foo(C())
}