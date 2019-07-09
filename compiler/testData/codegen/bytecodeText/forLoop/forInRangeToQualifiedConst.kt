object Host {
    const val M = 1
    const val N = 4
}

fun test(): Int {
    var s = 0
    for (i in Host.M .. Host.N) {
        s += i
    }
    return s
}

// 0 iterator
// 0 getStart
// 0 getEnd
// 0 getFirst
// 0 getLast
// 0 getStep
// 1 IF_ICMPGT
// 1 IF