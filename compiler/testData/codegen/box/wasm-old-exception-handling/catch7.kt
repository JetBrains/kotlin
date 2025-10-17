// WITH_STDLIB
// TARGET_BACKEND: WASM
// USE_OLD_EXCEPTION_HANDLING_PROPOSAL

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