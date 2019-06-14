// TARGET_BACKEND: JVM_IR
const val M = Long.MAX_VALUE

fun f(a: Long): Int {
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
// 2 LCMP
// 1 IFGT
// 1 IFLE
// 2 IF