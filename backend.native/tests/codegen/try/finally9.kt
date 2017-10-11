package codegen.`try`.finally9

import kotlin.test.*

@Test fun runTest() {
    do {
        try {
            break
        } finally {
            println("Finally 1")
        }
    } while (false)

    var stop = false
    while (!stop) {
        try {
            stop = true
            continue
        } finally {
            println("Finally 2")
        }
    }

    println("After")
}