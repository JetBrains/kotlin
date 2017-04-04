import kotlin.test.*

import kotlin.coroutines.experimental.buildSequence
import kotlin.coroutines.experimental.buildIterator

fun box() {
    val result = buildSequence {
        yieldAll(listOf(1, 2, 3).iterator())
    }
    assertEquals(listOf(1, 2, 3), result.toList())
}
