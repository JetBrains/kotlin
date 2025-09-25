// WITH_STDLIB
// TARGET_BACKEND: WASM
// USE_OLD_EXCEPTION_HANDLING_PROPOSAL

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