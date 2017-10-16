package codegen.innerClass.superOuter

import kotlin.test.*

open class Outer(val outer: String) {
    open inner class Inner(val inner: String): Outer(inner) {
        fun foo() = outer
    }

    fun value() = Inner("OK").foo()
}

fun box() = Outer("Fail").value()

@Test fun runTest() {
    println(box())
}