// WITH_RUNTIME
import kotlin.test.*

fun box(): String {
    var sum = 0
    for (i in (1 until 5).reversed()) {
        sum = sum * 10 + i
    }

    var sumL = 0L
    for (i in (1L until 5L).reversed()) {
        sumL = sumL * 10 + i
    }

    var sumC = 0
    for (i in ('1' until '5').reversed()) {
        sumC = sumC * 10 + i.toInt() - '0'.toInt()
    }

    return "OK"
}

// 0 reversed