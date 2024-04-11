// WITH_STDLIB
// TARGET_BACKEND: WASM
// USE_NEW_EXCEPTION_HANDLING_PROPOSAL
// TODO: remove the test when KT-66906 will be resolved

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