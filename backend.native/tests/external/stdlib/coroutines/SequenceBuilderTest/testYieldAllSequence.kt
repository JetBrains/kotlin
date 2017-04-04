import kotlin.test.*

import kotlin.coroutines.experimental.buildSequence
import kotlin.coroutines.experimental.buildIterator

fun box() {
    val result = buildSequence {
        yieldAll(sequenceOf(1, 2, 3))
    }
    assertEquals(listOf(1, 2, 3), result.toList())
}
