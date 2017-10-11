package codegen.`try`.try1

import kotlin.test.*

@Test fun runTest() {
    val x = try {
        5
    } catch (e: Throwable) {
        6
    }

    println(x)
}