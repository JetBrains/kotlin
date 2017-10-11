package codegen.boxing.boxing15

import kotlin.test.*

@Test fun runTest() {
    println(foo(17))
}

fun <T : Int> foo(x: T): Int = x