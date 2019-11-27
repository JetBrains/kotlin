// IGNORE_BACKEND_FIR: JVM_IR
// WITH_RUNTIME

import kotlin.test.assertEquals

fun box(): String {
    var result = 0
    if (1 == 1) {
        val x: Int? = 1
        result += x!!
    }

    assertEquals(1, result)
    return "OK"
}
