package runtime.basic.random

import kotlin.collections.*
import kotlin.random.*
import kotlin.system.*
import kotlin.test.*

/**
 * Tests that setting the same seed make random generate the same sequence
 */
private inline fun <reified T> testReproducibility(seed: Long, generator: () -> T) {
    // Reset seed. This will make Random to start a new sequence
    Random.seed = seed
    val first = Array<T>(50, { i -> generator() }).toList()

    // Reset seed and try again
    Random.seed = seed
    val second = Array<T>(50, { i -> generator() }).toList()
    assertTrue(first == second, "FAIL: got different sequences of generated values " +
            "first: $first, second: $second")
}

/**
 * Tests that setting seed makes random generate different sequence.
 */
private inline fun <reified T> testDifference(generator: () -> T) {
    Random.seed = 12345678L
    val first = Array<T>(100, { i -> generator() }).toList()

    Random.seed = 87654321L
    val second = Array<T>(100, { i -> generator() }).toList()
    assertTrue(first != second, "FAIL: got the same sequence of generated values " +
            "first: $first, second: $second")
}

@Test
fun testInts() {
    testReproducibility(getTimeMillis(), { Random.nextInt() })
    testReproducibility(Long.MAX_VALUE, { Random.nextInt() })
}

@Test
fun testLong() {
    testReproducibility(getTimeMillis(), { Random.nextLong() })
    testReproducibility(Long.MAX_VALUE, { Random.nextLong() })
}

@Test
fun testDiffInt() = testDifference { Random.nextInt() }

@Test
fun testDiffLong() = testDifference { Random.nextLong() }

@Test
fun testNextInt() {
    testReproducibility(getTimeMillis(), { Random.nextInt(1000) })
    testReproducibility(1000L, { Random.nextInt(1024) })
}

@Test
fun testBoundsNextInt() {
    boundTest(5000)
    boundTest(32)
    boundTest(2)
    boundTest(Int.MAX_VALUE)
}

private fun boundTest(bound: Int) {
    val a = Array<Int>(100, { Random.nextInt(bound) })
    a.forEach {
        assertTrue(it >= 0, "Should be: $it >= 0")
        assertTrue(it < bound, "Should be: $it < $bound")
    }
}
