package codegen.`try`.catch8

import kotlin.test.*

@Test fun runTest() {
    try {
        throw Error("Error happens")
    } catch (e: Throwable) {
        val message = e.message
        if (message != null) {
            println(message)
        }
    }
}