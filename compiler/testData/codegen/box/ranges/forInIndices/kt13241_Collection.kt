// IGNORE_BACKEND_FIR: JVM_IR
// KJS_WITH_FULL_RUNTIME
// WITH_RUNTIME

import kotlin.test.assertEquals

fun test(x: Any): Int {
    var sum = 0
    if (x is List<*>) {
        for (i in x.indices) {
            sum = sum * 10 + i
        }
    }
    return sum
}

fun box(): String {
    assertEquals(123, test(listOf(0, 0, 0, 0)))
    return "OK"
}