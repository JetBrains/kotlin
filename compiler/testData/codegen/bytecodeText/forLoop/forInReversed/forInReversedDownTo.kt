import kotlin.test.*

fun box(): String {
    var sum = 0
    for (i in (4 downTo 1).reversed()) {
        sum = sum * 10 + i
    }
    assertEquals(1234, sum)

    var sumL = 0L
    for (i in (4L downTo 1L).reversed()) {
        sumL = sumL * 10 + i
    }
    assertEquals(1234L, sumL)

    var sumC = 0
    for (i in ('4' downTo '1').reversed()) {
        sumC = sumC * 10 + i.toInt() - '0'.toInt()
    }
    assertEquals(1234, sumC)

    return "OK"
}

// 0 reversed
// 0 iterator
// 0 getStart
// 0 getEnd
// 0 getFirst
// 0 getLast
// 0 getStep
// 2 IF_ICMP[LG]T
// 1 IF[LG]T
// 3 IF
// 1 LCMP