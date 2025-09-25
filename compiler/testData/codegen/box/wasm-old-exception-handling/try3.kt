// WITH_STDLIB
// TARGET_BACKEND: WASM
// USE_OLD_EXCEPTION_HANDLING_PROPOSAL

import kotlin.test.*

fun box(): String {
    val x = try {
        throw Error()
    } catch (e: Throwable) {
        6
    }

    assertEquals(6, x)
    return "OK"
}