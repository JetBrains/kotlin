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

// JVM non-IR uses while.
// JVM IR uses if + do-while.

// 0 reversed
// 0 iterator
// 0 getStart
// 0 getEnd
// 0 getFirst
// 0 getLast
// 0 getStep

// JVM_TEMPLATES
// 1 IFLT
// 1 IF

// JVM_IR_TEMPLATES
// 1 IF_ICMPGT
// 1 IF_ICMPLE
// 2 IF