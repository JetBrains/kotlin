// IGNORE_BACKEND_FIR: JVM_IR
// KJS_WITH_FULL_RUNTIME
// WITH_RUNTIME

import kotlin.test.assertEquals

fun sumIndices(coll: Collection<*>?): Int {
    var sum = 0
    for (i in coll?.indices ?: return 0) {
        sum += i
    }
    return sum
}

fun box(): String {
    assertEquals(6, sumIndices(listOf(0, 0, 0, 0)))
    assertEquals(0, sumIndices(null))

    return "OK"
}