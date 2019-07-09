const val M = Long.MAX_VALUE

fun f(a: Long): Int {
    var n = 0
    for (i in a until M) {
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
// 1 LCMP
// 1 IFGE
// 1 IF