fun test(a: Char, b: Char): String {
    var s = ""
    for (i in a until b) {
        s += i
    }
    return s
}

// JVM non-IR uses while.
// JVM IR uses if + do-while. In addition, for "until" progressions, there is a check that the range is not empty: upper bound != MIN_VALUE.

// 0 iterator
// 0 getStart
// 0 getEnd
// 0 getFirst
// 0 getLast
// 0 getStep

// JVM_TEMPLATES
// 1 IF_ICMPGE
// 1 IF

// JVM_IR_TEMPLATES
// 1 IFEQ
// 1 IF_ICMPGT
// 1 IF_ICMPLE
// 3 IF