import kotlin.test.*

import kotlin.comparisons.*

fun box() {
    var count = 3

    val sequence = generateSequence {
        count--
        if (count >= 0) count else null
    }

    val list = sequence.toList()
    assertEquals(listOf(2, 1, 0), list)

    assertFails {
        sequence.toList()
    }
}
