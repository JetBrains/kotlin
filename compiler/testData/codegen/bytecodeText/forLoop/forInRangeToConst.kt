const val N = 42

fun test(): Int {
    var sum = 0
    for (i in 1 .. N) {
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
// 1 IF_ICMPGT
// 1 IF