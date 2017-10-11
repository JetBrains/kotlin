package codegen.`try`.try4

import kotlin.test.*

@Test fun runTest() {
    val x = try {
        println("Try")
        5
    } catch (e: Throwable) {
        throw e
    }

    println(x)
}