package runtime.basic.interface0

import kotlin.test.*

interface A {
    fun b() = c()
    fun c()
}

class B(): A {
    override fun c() {
        println("PASSED")
    }
}

@Test fun runTest() {
    val a:A = B()
    a.b()
}

