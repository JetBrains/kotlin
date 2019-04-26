// TARGET_BACKEND: JVM_IR
import kotlin.test.*

fun box(): String {
    val arr = intArrayOf(1, 1, 1, 1)
    var sum = 0
    for (i in arr.indices.reversed()) {
        sum = sum * 10 + i + arr[i]
    }
    assertEquals(4321, sum)

    return "OK"
}

// JVM non-IR uses while.
// JVM IR uses if + do-while.

// 0 reversed
// 0 iterator
// 0 getStart
// 0 getEnd
// 0 getFirst
// 0 getLast
// 0 getStep
// 1 IF_ICMPGT
// 1 IF_ICMPLE
// 2 IF