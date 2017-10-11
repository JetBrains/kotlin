package codegen.`try`.catch3

import kotlin.test.*

@Test fun runTest() {
    try {
        println("Before")
        throw Error("Error happens")
        println("After")
    } catch (e: Throwable) {
        println("Caught Throwable")
    }

    println("Done")
}