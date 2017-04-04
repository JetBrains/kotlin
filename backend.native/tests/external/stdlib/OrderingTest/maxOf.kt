import kotlin.test.*

import kotlin.comparisons.*

fun box() {
    assertEquals(Int.MAX_VALUE, maxOf(Int.MAX_VALUE, Int.MIN_VALUE))
    assertEquals(Int.MAX_VALUE, maxOf(Int.MAX_VALUE, Int.MIN_VALUE, 0))

    assertEquals(Long.MAX_VALUE, maxOf(Long.MAX_VALUE, Long.MIN_VALUE))
    assertEquals(Long.MAX_VALUE, maxOf(Long.MAX_VALUE, Long.MIN_VALUE, 0))

    assertEquals(Double.POSITIVE_INFINITY, maxOf(Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY))
    assertEquals(Double.POSITIVE_INFINITY, maxOf(Double.POSITIVE_INFINITY, Double.MAX_VALUE, Double.MIN_VALUE))
}
