import kotlin.test.*

import kotlin.comparisons.*

fun box() {
    val result = sequenceOf(1, 2).flatMap { (0..it).asSequence() }
    assertEquals(listOf(0, 1, 0, 1, 2), result.toList())
}
