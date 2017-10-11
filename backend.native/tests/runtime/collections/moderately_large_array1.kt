package runtime.collections.moderately_large_array1

import kotlin.test.*

@Test fun runTest() {
    val a = Array<Byte>(100000, { i -> i.toByte()})

    var sum = 0
    for (b in a) {
        sum += b
    }

    println(sum)
}

