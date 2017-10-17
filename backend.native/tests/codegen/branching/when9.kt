package codegen.branching.when9

import kotlin.test.*

@Test fun runTest() {
    foo(0)
    println("Ok")
}

fun foo(x: Int) {
    when (x) {
        0 -> 0
    }
}