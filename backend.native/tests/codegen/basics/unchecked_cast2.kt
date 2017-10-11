package codegen.basics.unchecked_cast2

import kotlin.test.*

@Test
fun runTest() {
    try {
        val x = cast<String>(Any())
        println(x.length)
    } catch (e: Throwable) {
        println("Ok")
    }
}

fun <T> cast(x: Any?) = x as T