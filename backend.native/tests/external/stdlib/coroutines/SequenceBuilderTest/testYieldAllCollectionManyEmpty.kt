import kotlin.test.*

import kotlin.coroutines.experimental.buildSequence
import kotlin.coroutines.experimental.buildIterator

fun box() {
    val result = buildSequence<Int> {
        yieldAll(listOf())
        yieldAll(listOf())
        yieldAll(listOf())
    }
    assertEquals(listOf(), result.toList())
}
