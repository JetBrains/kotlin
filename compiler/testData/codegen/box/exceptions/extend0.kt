// WITH_STDLIB

import kotlin.test.*

class C : Exception("OK")

fun box(): String {
    try {
        throw C()
    } catch (e: Throwable) {
        return e.message!!
    }
    return "FAIL"
}
