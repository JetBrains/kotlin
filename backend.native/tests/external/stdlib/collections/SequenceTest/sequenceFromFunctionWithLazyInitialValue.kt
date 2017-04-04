import kotlin.test.*

import kotlin.comparisons.*

fun box() {
    var start = 3
    val values = generateSequence({ start }, { n -> if (n > 0) n - 1 else null })
    val expected = listOf(3, 2, 1, 0)
    assertEquals(expected, values.toList())
    assertEquals(expected, values.toList(), "Iterating sequence second time yields the same result")

    start = 2
    assertEquals(expected.drop(1), values.toList(), "Initial value function is called on each iterator request")

    // does not throw on construction
    val errorValues = generateSequence<Int>({ (throw IllegalStateException()) }, { null })
    // does not throw on iteration
    val iterator = errorValues.iterator()
    // throws on advancing
    assertFails { iterator.next() }
}
