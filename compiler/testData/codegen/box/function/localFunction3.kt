// WITH_STDLIB

import kotlin.test.*

fun box(): String {
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
    return "OK"
}
