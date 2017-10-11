package codegen.lateinit.notInitialized

import kotlin.test.*

class A {
    lateinit var s: String

    fun foo() = s
}

@Test fun runTest() {
    val a = A()
    try {
        println(a.foo())
    }
    catch (e: RuntimeException) {
        println("OK")
        return
    }
    println("Fail")
}