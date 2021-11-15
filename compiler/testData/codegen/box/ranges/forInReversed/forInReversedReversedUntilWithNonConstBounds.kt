// WITH_STDLIB
import kotlin.test.*

fun intLow() = 1
fun intHigh() = 5
fun longLow() = 1L
fun longHigh() = 5L
fun charLow() = '1'
fun charHigh() = '5'

fun box(): String {
    var sum = 0
    for (i in (intLow() until intHigh()).reversed().reversed()) {
        sum = sum * 10 + i
    }
    assertEquals(1234, sum)

    var sumL = 0L
    for (i in (longLow() until longHigh()).reversed().reversed()) {
        sumL = sumL * 10 + i
    }
    assertEquals(1234, sumL)

    var sumC = 0
    for (i in (charLow() until charHigh()).reversed().reversed()) {
        sumC = sumC * 10 + i.toInt() - '0'.toInt()
    }
    assertEquals(1234, sumC)

    return "OK"
}