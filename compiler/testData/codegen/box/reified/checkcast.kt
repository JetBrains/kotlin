// IGNORE_BACKEND_FIR: JVM_IR
// WITH_RUNTIME

import kotlin.test.assertEquals

inline fun<reified T> checkcast(x: Any?): T {
    return x as T
}

fun box(): String {
    val x = checkcast<String>("abc")
    assertEquals("abc", x)
    val y = checkcast<Int>(1)
    assertEquals(1, y)

    try {
        val z = checkcast<Int>("abc")
    } catch (e: Exception) {
        return "OK"
    }

    return "Fail"
}
