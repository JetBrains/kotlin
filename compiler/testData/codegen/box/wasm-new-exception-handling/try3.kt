// WITH_STDLIB
// TARGET_BACKEND: WASM
// USE_NEW_EXCEPTION_HANDLING_PROPOSAL
// TODO: remove the test when KT-66906 will be resolved

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