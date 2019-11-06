fun <T : CharSequence> test(s: T): Int {
    var result = 0
    for (i in s.indices) {
        result = result * 10 + (i + 1)
    }
    return result
}

// JVM non-IR uses while.
// JVM IR uses if + do-while.

// 0 iterator
// 0 getStart
// 0 getEnd
// 0 getFirst
// 0 getLast
// 1 INVOKEINTERFACE java/lang/CharSequence\.length \(\)I

// JVM_TEMPLATES
// 0 IF_ICMPGT
// 0 IF_ICMPEQ
// 1 IF_ICMPGE
// 1 IF

// JVM_IR_TEMPLATES
// 1 IF_ICMPGT
// 1 IF_ICMPLE
// 2 IF
