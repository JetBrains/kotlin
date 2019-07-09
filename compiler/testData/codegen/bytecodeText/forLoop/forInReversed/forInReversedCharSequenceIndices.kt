// IGNORE_BACKEND: JVM_IR
// NOTE: Enable once CharSequence.indices is handled
import kotlin.test.*

fun box(): String {
    val cs = "1111"
    var sum = 0
    for (i in cs.indices.reversed()) {
        sum = sum * 10 + i + cs[i].toInt() - '0'.toInt()
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