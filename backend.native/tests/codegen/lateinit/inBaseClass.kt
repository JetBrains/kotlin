package codegen.lateinit.inBaseClass

import kotlin.test.*

class A(val a: Int)

open class B {
    lateinit var a: A
}

class C: B() {
    fun foo() { a = A(42) }
}

@Test fun runTest() {
    val c = C()
    c.foo()
    println(c.a.a)
}
