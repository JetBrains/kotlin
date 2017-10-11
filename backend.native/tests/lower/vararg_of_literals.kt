package lower.vararg_of_literals

import kotlin.test.*

@Test fun runTest() {
    foo()
    foo()
}

fun foo() {
    val array = arrayOf("a", "b")
    println(array[0])
    array[0] = "42"
}