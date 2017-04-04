import kotlin.test.*

import kotlin.coroutines.experimental.buildSequence
import kotlin.coroutines.experimental.buildIterator

fun box() {
    val result = buildSequence {
        yieldAll(listOf(1, 2, 3))
        yield(4)
    }
    assertEquals(listOf(1, 2, 3, 4), result.toList())
}
