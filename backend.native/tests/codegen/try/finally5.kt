package codegen.`try`.finally5

import kotlin.test.*

@Test fun runTest() {
    println(foo())
}

fun foo(): Int {
    try {
        println("Done")
        return 0
    } finally {
        println("Finally")
    }

    println("After")
    return 1
}