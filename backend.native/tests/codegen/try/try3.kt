package codegen.`try`.try3

import kotlin.test.*

@Test fun runTest() {
    val x = try {
        throw Error()
    } catch (e: Throwable) {
        6
    }

    println(x)
}