// WITH_STDLIB

import kotlin.test.*

fun box(): String {
    val FALSE: Boolean? = false

    if (FALSE != null) {
        do {
            return "OK"
        } while (FALSE)
    }
    return "FAIL"
}
