// WITH_STDLIB

import kotlin.test.*

fun box(): String {
    assertFailsWith<IllegalStateException>("My error") {
        try {
            error("My error")
        } catch (e: Throwable) {
            throw e
        }
    }
    return "OK"
}
