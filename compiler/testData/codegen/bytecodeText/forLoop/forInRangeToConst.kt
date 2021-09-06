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

// JVM_TEMPLATES
// 1 IF_ICMPGT
// 1 IF
// 5 ILOAD
// 4 ISTORE
// 1 IINC

// JVM_IR_TEMPLATES
// 1 IF_ICMPGE
// 1 IF
// 4 ILOAD
// 3 ISTORE
// 1 IADD
// 0 ISUB
// 1 IINC