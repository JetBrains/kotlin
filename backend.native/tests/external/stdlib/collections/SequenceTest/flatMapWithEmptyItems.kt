import kotlin.test.*

import kotlin.comparisons.*

fun box() {
    val result = sequenceOf(1, 2, 4).flatMap { if (it == 2) sequenceOf<Int>() else (it - 1..it).asSequence() }
    assertEquals(listOf(0, 1, 3, 4), result.toList())
}
