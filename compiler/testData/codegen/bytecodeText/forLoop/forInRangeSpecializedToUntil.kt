fun test(): Int {
    val intArray = intArrayOf(1, 2, 3)
    var sum = 0
    for (i in 0..intArray.size - 1) {
        sum += intArray[i]
    }
    return sum
}

// 0 iterator
// 0 getStart
// 0 getEnd
// 0 getFirst
// 0 getLast
// 0 getStep

// JVM non-IR uses while.
// JVM IR uses if + do-while.

// JVM_TEMPLATES
// 1 IF_ICMPGE
// 1 IF

// JVM_IR_TEMPLATES
// 0 IF_ICMPGT
// 1 IF_ICMPGE
// 0 IF_ICMPEQ
// 1 IF
// 5 ILOAD
// 4 ISTORE
// 1 IINC
// 1 IADD
// 0 ISUB
