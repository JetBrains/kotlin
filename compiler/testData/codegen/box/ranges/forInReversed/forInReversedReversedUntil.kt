// WITH_STDLIB
import kotlin.test.*

fun box(): String {
    var sum = 0
    for (i in (1 until 5).reversed().reversed()) {
        sum = sum * 10 + i
    }
    assertEquals(1234, sum)

    var sumL = 0L
    for (i in (1L until 5L).reversed().reversed()) {
        sumL = sumL * 10 + i
    }
    assertEquals(1234, sumL)

    var sumC = 0
    for (i in ('1' until '5').reversed().reversed()) {
        sumC = sumC * 10 + i.toInt() - '0'.toInt()
    }
    assertEquals(1234, sumC)

    return "OK"
}