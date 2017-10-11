package codegen.`try`.finally4

import kotlin.test.*

@Test fun runTest() {

    try {
        try {
            println("Try")
            throw Error("Error happens")
            println("After throw")
        } catch (e: Error) {
            println("Catch")
            throw Exception()
            println("After throw")
        } finally {
            println("Finally")
        }

        println("After nested try")

    } catch (e: Error) {
        println("Caught Error")
    } catch (e: Exception) {
        println("Caught Exception")
    }

    println("Done")
}