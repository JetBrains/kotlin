package codegen.`try`.try2

import kotlin.test.*

@Test fun runTest() {
    val x = try {
        throw Error()
        5
    } catch (e: Throwable) {
        6
    }

    println(x)
}