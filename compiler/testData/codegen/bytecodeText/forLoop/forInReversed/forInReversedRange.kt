import kotlin.test.*

fun intRange() = 1 .. 4
fun longRange() = 1L .. 4L
fun charRange() = '1' .. '4'

fun box(): String {
    var sum = 0
    for (i in intRange().reversed()) {
        sum = sum * 10 + i
    }
    assertEquals(4321, sum)

    var sumL = 0L
    for (i in longRange().reversed()) {
        sumL = sumL * 10 + i
    }
    assertEquals(4321L, sumL)

    var sumC = 0
    for (i in charRange().reversed()) {
        sumC = sumC * 10 + i.toInt() - '0'.toInt()
    }
    assertEquals(4321, sumC)

    return "OK"
}

// 0 reversed
// 0 iterator
// 0 getStart
// 0 getEnd
// 3 getFirst
// 3 getLast
