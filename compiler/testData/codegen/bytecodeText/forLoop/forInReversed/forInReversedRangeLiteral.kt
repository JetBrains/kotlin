import kotlin.test.*

fun box(): String {
    var sum = 0
    for (i in (1 .. 4).reversed()) {
        sum = sum * 10 + i
    }
    assertEquals(4321, sum)

    var sumL = 0L
    for (i in (1L .. 4L).reversed()) {
        sumL = sumL * 10 + i
    }
    assertEquals(4321L, sumL)

    var sumC = 0
    for (i in ('1' .. '4').reversed()) {
        sumC = sumC * 10 + i.toInt() - '0'.toInt()
    }
    assertEquals(4321, sumC)

    return "OK"
}

// JVM non-IR uses while.
// JVM IR uses if + do-while. The surrounding "if" gets optimized in this test (constant condition), except for Long.

// 0 reversed
// 0 iterator
// 0 getStart
// 0 getEnd
// 0 getFirst
// 0 getLast
// 0 getStep

// JVM_TEMPLATES
// 2 IF_ICMPLT
// 1 IFLT
// 3 IF
// 1 LCMP

// JVM_IR_TEMPLATES
// 2 IF_ICMPLE
// 1 IFGT
// 1 IFLE
// 4 IF
// 2 LCMP