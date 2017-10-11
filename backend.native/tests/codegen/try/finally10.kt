package codegen.`try`.finally10

import kotlin.test.*

@Test fun runTest() {
    while (true) {
        try {
            continue
        } finally {
            println("Finally")
            break
        }
    }

    println("After")
}