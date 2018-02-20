package codegen.lateinit.isInitialized

import kotlin.test.*

class A {
    lateinit var s: String

    fun foo() {
        println(::s.isInitialized)
    }
}

@Test fun runTest() {
    val a = A()
    a.foo()
    a.s = "zzz"
    a.foo()
}