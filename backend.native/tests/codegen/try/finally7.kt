package codegen.`try`.finally7

import kotlin.test.*

@Test fun runTest() {
    println(foo())
}

fun foo(): Int {
    try {
        println("Done")
        throw Error()
    } finally {
        println("Finally")
        return 1
    }

    println("After")
    return 2
}