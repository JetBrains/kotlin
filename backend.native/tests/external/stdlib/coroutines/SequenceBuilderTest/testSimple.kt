import kotlin.test.*

import kotlin.coroutines.experimental.buildSequence
import kotlin.coroutines.experimental.buildIterator

fun box() {
    val result = buildSequence {
        for (i in 1..3) {
            yield(2 * i)
        }
    }

    assertEquals(listOf(2, 4, 6), result.toList())
    // Repeated calls also work
    assertEquals(listOf(2, 4, 6), result.toList())
}
