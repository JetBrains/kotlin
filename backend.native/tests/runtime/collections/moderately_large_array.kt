package runtime.collections.moderately_large_array

import kotlin.test.*

@Test fun runTest() {
    val a = ByteArray(1000000)

    var sum = 0
    for (b in a) {
        sum += b
    }

    println(sum)
}

