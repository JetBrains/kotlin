package codegen.lateinit.initialized

import kotlin.test.*

class A {
    lateinit var s: String

    fun foo() = s
}

@Test fun runTest() {
    val a = A()
    a.s = "zzz"
    println(a.foo())
}