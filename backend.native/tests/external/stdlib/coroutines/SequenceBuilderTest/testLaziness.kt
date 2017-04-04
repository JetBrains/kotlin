import kotlin.test.*

import kotlin.coroutines.experimental.buildSequence
import kotlin.coroutines.experimental.buildIterator

fun box() {
    var sharedVar = -2
    val result = buildSequence {
        while (true) {
            when (sharedVar) {
                -1 -> return@buildSequence
                -2 -> error("Invalid state: -2")
                else -> yield(sharedVar)
            }
        }
    }

    val iterator = result.iterator()

    sharedVar = 1
    assertTrue(iterator.hasNext())
    assertEquals(1, iterator.next())

    sharedVar = 2
    assertTrue(iterator.hasNext())
    assertEquals(2, iterator.next())

    sharedVar = 3
    assertTrue(iterator.hasNext())
    assertEquals(3, iterator.next())

    sharedVar = -1
    assertFalse(iterator.hasNext())
    assertFailsWith<NoSuchElementException> { iterator.next() }
}
