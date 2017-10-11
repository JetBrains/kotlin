package codegen.`try`.finally1

import kotlin.test.*

@Test fun runTest() {

    try {
        println("Try")
    } finally {
        println("Finally")
    }

    println("Done")
}