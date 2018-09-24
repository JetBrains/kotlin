// WITH_RUNTIME
import kotlin.test.*

fun intLow() = 1
fun intHigh() = 4
fun longLow() = 1L
fun longHigh() = 4L
fun charLow() = '1'
fun charHigh() = '4'

fun box(): String {
    var sum = 0
    for (i in (intLow() .. intHigh()).reversed()) {
        sum = sum * 10 + i
    }
    assertEquals(4321, sum)

    var sumL = 0L
    for (i in (longLow() .. longHigh()).reversed()) {
        sumL = sumL * 10 + i
    }
    assertEquals(4321L, sumL)

    var sumC = 0
    for (i in (charLow() .. charHigh()).reversed()) {
        sumC = sumC * 10 + i.toInt() - '0'.toInt()
    }
    assertEquals(4321, sumC)

    return "OK"
}