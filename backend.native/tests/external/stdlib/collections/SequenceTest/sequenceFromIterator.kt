import kotlin.test.*

import kotlin.comparisons.*

fun box() {
    val list = listOf(3, 2, 1, 0)
    val iterator = list.iterator()
    val sequence = iterator.asSequence()
    assertEquals(list, sequence.toList())
    assertFails {
        sequence.toList()
    }
}
