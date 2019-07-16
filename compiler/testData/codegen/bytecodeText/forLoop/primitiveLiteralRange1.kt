fun f() {
    for (i in 1..2) {
    }
}

// JVM non-IR uses while.
// JVM IR uses if + do-while. The surrounding "if" gets optimized in this test (constant condition).

// 0 iterator
// 0 getStart
// 0 getEnd
// 0 getFirst
// 0 getLast
// 0 getStep
// 1 IF

// JVM_TEMPLATES
// 1 IF_ICMPGT

// JVM_IR_TEMPLATES
// 1 IF_ICMPLE