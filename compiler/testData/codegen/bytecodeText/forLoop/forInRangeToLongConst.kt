const val N = 42L

fun test(): Long {
    var sum = 0L
    for (i in 1L .. N) {
        sum += i
    }
    return sum
}

// 0 iterator
// 0 getStart
// 0 getEnd
// 0 getFirst
// 0 getLast
// 0 getStep
// 1 LCMP
// 1 IFGT
// 1 IF
// 0 L2I
// 0 I2L