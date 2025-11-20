// WITH_STDLIB

import kotlin.test.*

fun box(): String {
    val x = try {
        5
    } catch (e: Throwable) {
        6
    }

    assertEquals(5, x)
    return "OK"
}
