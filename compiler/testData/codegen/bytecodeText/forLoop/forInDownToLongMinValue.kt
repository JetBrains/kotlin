const val M = Long.MIN_VALUE

fun f(a: Long) {
    for (i in a downTo M) {
    }
}

// 0 iterator
// 0 getStart
// 0 getEnd
// 0 getFirst
// 0 getLast
// 0 getStep
// 2 LCMP
// 2 IF