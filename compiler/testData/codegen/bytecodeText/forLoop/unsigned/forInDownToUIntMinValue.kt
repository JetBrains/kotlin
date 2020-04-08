// WITH_RUNTIME
const val M = UInt.MIN_VALUE

fun f(a: UInt): Int {
    var n = 0
    for (i in a downTo M) {
        n++
    }
    return n
}

// 0 iterator
// 0 getStart
// 0 getEnd
// 0 getFirst
// 0 getLast
// 0 getStep
// 1 INVOKESTATIC kotlin/UnsignedKt.uintCompare
// 2 IF