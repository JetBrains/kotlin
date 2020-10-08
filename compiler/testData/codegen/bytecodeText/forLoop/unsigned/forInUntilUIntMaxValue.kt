// WITH_RUNTIME
const val M = UInt.MAX_VALUE

fun f(a: UInt): Int {
    var n = 0
    for (i in a until M) {
        n++
    }
    return n
}

// JVM non-IR uses while.
// JVM IR uses if + do-while.

// 0 iterator
// 0 getStart
// 0 getEnd
// 0 getFirst
// 0 getLast
// 0 getStep
// 0 INVOKESTATIC kotlin/UInt.constructor-impl
// 0 INVOKE\w+ kotlin/UInt.(un)?box-impl

// JVM_TEMPLATES
// 1 INVOKESTATIC kotlin/UnsignedKt.uintCompare
// 1 IFGE
// 1 IF

// JVM_IR_TEMPLATES
// 2 INVOKESTATIC kotlin/UnsignedKt.uintCompare
// 1 IFGE
// 1 IFLT
// 2 IF