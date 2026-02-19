// WITH_STDLIB

import kotlin.test.*

val sb = StringBuilder()

fun box(): String {
    try {
        foo()
    } catch (e: Throwable) {
        val message = e.message
        if (message != null) {
            sb.append(message)
        }
    }

    return sb.toString()
}

fun foo() {
    throw Error("OK")
}
