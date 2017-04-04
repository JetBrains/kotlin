import kotlin.test.*

import kotlin.coroutines.experimental.buildSequence
import kotlin.coroutines.experimental.buildIterator

fun box() {
    val result = buildSequence<Int> {}
    val iterator = result.iterator()

    assertFalse(iterator.hasNext())
    assertFalse(iterator.hasNext())

    assertFailsWith<NoSuchElementException> { iterator.next() }
}
