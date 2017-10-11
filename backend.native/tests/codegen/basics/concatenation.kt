package codegen.basics.concatenation

import kotlin.test.*

@Test
fun runTest() {
    val s = "world"
    val i = 1
    println("Hello $s $i ${2*i}")

    for (item in listOf("a", "b")) {
        println("Hello, $item")
    }
}