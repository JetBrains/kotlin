package runtime.collections.array3

import kotlin.test.*

import konan.*

@Test fun runTest() {
    val data = immutableBinaryBlobOf(0x1, 0x2, 0x3, 0x7, 0x8, 0x9, 0x80, 0xff)
    for (b in data) {
        print("$b ")
    }
    println()

    val dataClone = data.toByteArray()
    dataClone.map { print("$it ") }
    println()
}