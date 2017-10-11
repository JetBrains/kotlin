package codegen.controlflow.unreachable1

import kotlin.test.*

@Test fun runTest() {
    println(foo())
}

fun foo(): Int {
    return 1
    println("After return")
}