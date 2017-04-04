import kotlin.test.*

import kotlin.comparisons.*

fun box() {
    val values = generateSequence(3) { n -> if (n > 0) n - 1 else null }
    val expected = listOf(3, 2, 1, 0)
    assertEquals(expected, values.toList())
    assertEquals(expected, values.toList(), "Iterating sequence second time yields the same result")
}
