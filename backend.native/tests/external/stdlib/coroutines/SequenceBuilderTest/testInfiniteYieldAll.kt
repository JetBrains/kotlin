import kotlin.test.*

import kotlin.coroutines.experimental.buildSequence
import kotlin.coroutines.experimental.buildIterator

fun box() {
    val values = buildIterator {
        while (true) {
            yieldAll((1..5).map { it })
        }
    }

    var sum = 0
    repeat(10) {
        sum += values.next() //.also(::println)
    }
    assertEquals(30, sum)
}
