// IGNORE_BACKEND: JVM_IR
import kotlin.test.assertEquals

fun test(coll: Collection<*>?): Int {
    var sum = 0
    for (i in coll?.indices ?: return 0) {
        sum += i
    }
    return sum
}

// 0 iterator
// 0 getStart
// 0 getEnd
// 1 getIndices
// 1 getFirst
// 1 getLast
