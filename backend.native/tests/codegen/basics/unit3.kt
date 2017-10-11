package codegen.basics.unit3

import kotlin.test.*

@Test
fun runTest() {
    foo(Unit)
}

fun foo(x: Any) {
    println(x.toString())
}