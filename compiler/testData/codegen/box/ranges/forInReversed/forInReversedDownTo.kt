// WITH_RUNTIME
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