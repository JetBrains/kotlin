import kotlin.test.*

import kotlin.coroutines.experimental.buildSequence
import kotlin.coroutines.experimental.buildIterator

fun box() {
    var inc = 0
    val result = buildSequence {
        for (i in 1..3) {
            inc++
            yield(inc * i)
        }
    }

    assertEquals(listOf(Pair(1, 2), Pair(6, 8), Pair(15, 18)), result.zip(result).toList())
}
