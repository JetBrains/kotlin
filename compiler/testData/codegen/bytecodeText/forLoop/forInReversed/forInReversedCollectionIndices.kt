// IGNORE_BACKEND: JVM_IR
// NOTE: Enable once Collection.indices is handled
import kotlin.test.*

fun box(): String {
    val xs = listOf(1, 1, 1, 1)
    var sum = 0
    for (i in xs.indices.reversed()) {
        sum = sum * 10 + i + xs[i]
    }
    assertEquals(4321, sum)

    return "OK"
}

// 0 reversed
// 0 iterator
// 0 getStart
// 0 getEnd
// 0 getFirst
// 0 getLast
// 0 getStep
// 1 IF(_ICMPG|L)T
// 1 IF