package runtime.collections.sort0

import kotlin.test.*

@Test fun runTest() {
    println(arrayOf("x", "a", "b").sorted().toString())
    println(intArrayOf(239, 42, -1, 100500, 0).sorted().toString());
}
