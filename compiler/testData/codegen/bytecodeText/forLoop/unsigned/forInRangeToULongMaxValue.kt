// WITH_RUNTIME
const val M = ULong.MAX_VALUE

fun f(a: ULong): Int {
    var n = 0
    for (i in a..M) {
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
// 1 INVOKESTATIC kotlin/UnsignedKt.ulongCompare
// 2 IF
// 0 INVOKESTATIC kotlin/ULong.constructor-impl
// 0 INVOKE\w+ kotlin/ULong.(un)?box-impl
