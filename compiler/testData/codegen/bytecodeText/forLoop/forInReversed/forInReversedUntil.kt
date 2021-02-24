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

// JVM non-IR optimizes out all reversed() calls. However, this has a bug (KT-42533).
// JVM IR optimizes out the 2nd reversed() call.

// 0 iterator
// 0 getStart
// 0 getEnd

// JVM_TEMPLATES
// 0 reversed
// 0 getFirst
// 0 getLast
// 0 getStep
// 2 IF_ICMPLT
// 1 IFLT
// 3 IF
// 1 LCMP

// JVM_IR_TEMPLATES
// 3 reversed
// 3 getFirst
// 3 getLast
// 3 getStep