// WITH_STDLIB
// TARGET_BACKEND: WASM
// USE_OLD_EXCEPTION_HANDLING_PROPOSAL

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