// TARGET_BACKEND: JVM_IR
fun test() {
    var sum = 0
    for (i in intArrayOf(0, 0, 0, 0).indices) {
        sum += i
    }
}

// JVM non-IR uses while.
// JVM IR uses if + do-while.

// 0 iterator
// 0 getStart
// 0 getEnd
// 0 getFirst
// 0 getLast
// 1 IF_ICMPGT
// 1 IF_ICMPLE
// 2 IF