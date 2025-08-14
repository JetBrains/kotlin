// WITH_STDLIB
// TARGET_BACKEND: WASM
// USE_NEW_EXCEPTION_HANDLING_PROPOSAL
// WASM_FAILS_IN_SINGLE_MODULE_MODE
// TODO: remove the test when KT-66906 will be resolved

import kotlin.test.*

fun box(): String {
    val x = try {
        throw Error()
        5
    } catch (e: Throwable) {
        6
    }

    assertEquals(6, x)
    return "OK"
}