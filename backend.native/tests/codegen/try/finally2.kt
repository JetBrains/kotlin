package codegen.`try`.finally2

import kotlin.test.*

@Test fun runTest() {

    try {
        println("Try")
        throw Error("Error happens")
        println("After throw")
    } catch (e: Error) {
        println("Caught Error")
    } finally {
        println("Finally")
    }

    println("Done")
}