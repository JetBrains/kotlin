fun <T : Collection<*>> test(c: T) {
    var sum = 0
    for (i in c.indices) {
        sum += i
    }
}

// 0 iterator
// 0 getStart
// 0 getEnd
// 0 getFirst
// 0 getLast
// 1 INVOKEINTERFACE java/util/Collection\.size \(\)I

// JVM non-IR uses while.
// JVM IR uses if + do-while.

// JVM_TEMPLATES
// 1 IF_ICMPGE
// 1 IF

// JVM_IR_TEMPLATES
// 1 IF_ICMPGT
// 1 IF_ICMPLE
// 2 IF
