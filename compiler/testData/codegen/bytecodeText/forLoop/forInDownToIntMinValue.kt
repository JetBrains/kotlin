const val M = Int.MIN_VALUE

fun f(a: Int): Int {
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
// 2 IF_ICMP
// 2 IF