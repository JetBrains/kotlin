package runtime.collections.array2

import kotlin.test.*

@Test fun runTest() {
    val byteArray = Array<Byte>(5, { i -> (i * 2).toByte() })
    byteArray.map { println(it) }

    val intArray = Array<Int>(5, { i -> i * 4 })
    println(intArray.sum())
}