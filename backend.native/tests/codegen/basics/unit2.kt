package codegen.basics.unit2

import kotlin.test.*

@Test
fun runTest() {
    val x = foo()
    println(x.toString())
}

fun foo() {
    return Unit
}