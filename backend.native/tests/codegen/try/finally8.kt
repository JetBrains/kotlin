package codegen.`try`.finally8

import kotlin.test.*

@Test fun runTest() {
    println(foo())
}

fun foo(): Int {
    try {
        try {
            return 42
        } finally {
            println("Finally 1")
        }
    } finally {
        println("Finally 2")
    }

    println("After")
    return 2
}