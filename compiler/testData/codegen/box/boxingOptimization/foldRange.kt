// IGNORE_BACKEND_FIR: JVM_IR
// KJS_WITH_FULL_RUNTIME
// WITH_RUNTIME

import kotlin.test.assertEquals

fun box(): String {
    val result = (1..5).fold(0) { x, y -> x + y }

    assertEquals(15, result)

    return "OK"
}
