// WITH_RUNTIME
import kotlin.test.*

fun intRange() = 1 .. 4
fun longRange() = 1L .. 4L
fun charRange() = '1' .. '4'

fun box(): String {
    var sum = 0
    for (i in intRange().reversed().reversed().reversed()) {
        sum = sum * 10 + i
    }
    assertEquals(4321, sum)

    var sumL = 0L
    for (i in longRange().reversed().reversed().reversed()) {
        sumL = sumL * 10 + i
    }
    assertEquals(4321L, sumL)

    var sumC = 0
    for (i in charRange().reversed().reversed().reversed()) {
        sumC = sumC * 10 + i.toInt() - '0'.toInt()
    }
    assertEquals(4321, sumC)

    return "OK"
}