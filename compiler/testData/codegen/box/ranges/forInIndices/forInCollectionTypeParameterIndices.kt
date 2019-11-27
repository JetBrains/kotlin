// IGNORE_BACKEND_FIR: JVM_IR
// KJS_WITH_FULL_RUNTIME
// WITH_RUNTIME

import kotlin.test.assertEquals

fun <T : Collection<*>> sumIndices(c: T): Int {
    var sum = 0
    for (i in c.indices) {
        sum += i
    }
    return sum
}

fun box(): String {
    val list = listOf(0, 0, 0, 0)
    val sum = sumIndices(list)
    assertEquals(6, sum)

    return "OK"
}