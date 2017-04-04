import kotlin.test.*

import kotlin.comparisons.*

fun box() {
    assertEquals(Int.MIN_VALUE, minOf(Int.MAX_VALUE, Int.MIN_VALUE))
    assertEquals(Int.MIN_VALUE, minOf(Int.MAX_VALUE, Int.MIN_VALUE, 0))

    assertEquals(Long.MIN_VALUE, minOf(Long.MAX_VALUE, Long.MIN_VALUE))
    assertEquals(Long.MIN_VALUE, minOf(Long.MAX_VALUE, Long.MIN_VALUE, 0))

    assertEquals(Double.NEGATIVE_INFINITY, minOf(Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY))
    assertEquals(Double.MIN_VALUE, minOf(Double.POSITIVE_INFINITY, Double.MAX_VALUE, Double.MIN_VALUE))
}
