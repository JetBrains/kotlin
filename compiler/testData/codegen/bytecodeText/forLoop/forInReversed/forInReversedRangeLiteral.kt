// WITH_RUNTIME
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

// 0 reversed
// 0 getFirst
// 0 getLast
// 0 getStep