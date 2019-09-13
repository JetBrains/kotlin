fun test() {
    var sum = 0
    for (i in listOf(0, 0, 0, 0).indices) {
        sum += i
    }
}

// 0 iterator
// 0 getStart
// 0 getEnd
// 0 getFirst
// 0 getLast

// JVM non-IR uses while.
// JVM IR uses if + do-while.

// JVM_TEMPLATES
// 1 IF_ICMPGE
// 1 IF

// JVM_IR_TEMPLATES
// 1 IF_ICMPGT
// 1 IF_ICMPLE
// 2 IF
