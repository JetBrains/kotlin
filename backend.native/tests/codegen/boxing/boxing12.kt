package codegen.boxing.boxing12

import kotlin.test.*

fun foo(x: Number) {
    println(x.toByte())
}

@Test fun runTest() {
    foo(18)
}