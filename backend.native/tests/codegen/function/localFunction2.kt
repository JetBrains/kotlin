package codegen.function.localFunction2

import kotlin.test.*

@Test fun runTest() {
    var a = 0
    fun local() {
        class A {
            val b = 0
            fun f() {
                a = b
            }

        }
        fun local2() : A {
            return A()
        }
    }
    println("OK")
}