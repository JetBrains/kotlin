// WITH_RUNTIME
import kotlin.test.*

fun intLow() = 1
fun intHigh() = 5
fun longLow() = 1L
fun longHigh() = 5L
fun charLow() = '1'
fun charHigh() = '5'

fun box(): String {
    var sum = 0
    for (i in (intLow() until intHigh()).reversed()) {
        sum = sum * 10 + i
    }
    assertEquals(4321, sum)

    var sumL = 0L
    for (i in (longLow() until longHigh()).reversed()) {
        sumL = sumL * 10 + i
    }
    assertEquals(4321L, sumL)

    var sumC = 0
    for (i in (charLow() until charHigh()).reversed()) {
        sumC = sumC * 10 + i.toInt() - '0'.toInt()
    }
    assertEquals(4321, sumC)

    return "OK"
}