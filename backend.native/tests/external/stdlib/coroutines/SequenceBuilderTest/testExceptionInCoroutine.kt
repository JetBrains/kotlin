import kotlin.test.*

import kotlin.coroutines.experimental.buildSequence
import kotlin.coroutines.experimental.buildIterator

fun box() {
    var sharedVar = -2
    val result = buildSequence {
        while (true) {
            when (sharedVar) {
                -1 -> return@buildSequence
                -2 -> throw UnsupportedOperationException("-2 is unsupported")
                else -> yield(sharedVar)
            }
        }
    }

    val iterator = result.iterator()

    sharedVar = 1
    assertEquals(1, iterator.next())

    sharedVar = -2
    assertFailsWith<UnsupportedOperationException> { iterator.hasNext() }
    assertFailsWith<IllegalStateException> { iterator.hasNext() }
    assertFailsWith<IllegalStateException> { iterator.next() }
}
