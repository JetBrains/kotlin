import kotlin.test.*

import kotlin.coroutines.experimental.buildSequence
import kotlin.coroutines.experimental.buildIterator

fun box() {
    val result = buildSequence {
        yield(0)
        yieldAll(listOf(1, 2, 3))
        yield(4)
        yield(5)
        yieldAll(listOf(6))
        yield(7)
        yieldAll(listOf())
        yield(8)
    }
    assertEquals(listOf(0, 1, 2, 3, 4, 5, 6, 7, 8), result.toList())
}
