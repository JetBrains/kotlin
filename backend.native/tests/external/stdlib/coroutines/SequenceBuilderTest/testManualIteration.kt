import kotlin.test.*

import kotlin.coroutines.experimental.buildSequence
import kotlin.coroutines.experimental.buildIterator

fun box() {
    val result = buildSequence {
        yield(1)
        yield(2)
        yield(3)
    }

    val iterator = result.iterator()

    assertTrue(iterator.hasNext())
    assertTrue(iterator.hasNext())
    assertEquals(1, iterator.next())

    assertTrue(iterator.hasNext())
    assertTrue(iterator.hasNext())
    assertEquals(2, iterator.next())

    assertEquals(3, iterator.next())

    assertFalse(iterator.hasNext())
    assertFalse(iterator.hasNext())

    assertFailsWith<NoSuchElementException> { iterator.next() }

    assertEquals(1, result.iterator().next())
}
