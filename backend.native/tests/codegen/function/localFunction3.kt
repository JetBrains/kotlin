package codegen.function.localFunction3

import kotlin.test.*

@Test fun runTest() {
    fun bar() {
        fun local1() {
            bar()
        }
        local1()

        var x = 0
        fun local2() {
            x++
            bar()
        }
        local2()
    }
    println("OK")
}