import kotlin.test.*

import kotlin.coroutines.experimental.buildSequence
import kotlin.coroutines.experimental.buildIterator

fun box() {
    val result = buildSequence {
        yield(1)
    }

    val iterator = result.iterator()

    assertTrue(iterator.hasNext())
    assertTrue(iterator.hasNext())
    assertTrue(iterator.hasNext())

    assertEquals(1, iterator.next())

    assertFalse(iterator.hasNext())
    assertFalse(iterator.hasNext())
    assertFalse(iterator.hasNext())

    assertFailsWith<NoSuchElementException> { iterator.next() }
}
