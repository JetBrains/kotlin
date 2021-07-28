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

// 0 L2I
// 0 I2L

// JVM_TEMPLATES
// 1 IFGT
// 1 LCMP
// 1 IF

// JVM_IR_TEMPLATES
// 1 LCMP
// 1 IFGE
// 1 IF