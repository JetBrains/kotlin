package codegen.`try`.finally11

import kotlin.test.*

@Test fun runTest() {
    try {
        try {
            return
        } catch (e: Error) {
            println("Catch 1")
        } finally {
            println("Finally")
            throw Error()
        }
    } catch (e: Error) {
        println("Catch 2")
    }

    println("Done")
}