// WITH_STDLIB

import kotlin.test.*

fun box(): String {
    assertFailsWith<IllegalStateException>("My another error") {
        try {
            error("My error")
        } catch (e: Throwable) {
            error("My another error")
        }
    }
    return "OK"
}
