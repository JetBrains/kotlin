package codegen.localClass.localFunctionInLocalClass

import kotlin.test.*

@Test fun runTest() {
    var x = 0
    class A {
        fun bar() {
            fun local() {
                class B {
                    fun baz() {
                        fun local2() {
                            x++
                        }
                        local2()
                    }
                }
                B().baz()
            }
            local()
        }
    }
    A().bar()
    println("OK")
}