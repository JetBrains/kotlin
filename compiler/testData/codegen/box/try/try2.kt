// WITH_STDLIB

import kotlin.test.*

fun box(): String {
    val x = try {
        throw Error()
        5
    } catch (e: Throwable) {
        6
    }

    assertEquals(6, x)
    return "OK"
}
