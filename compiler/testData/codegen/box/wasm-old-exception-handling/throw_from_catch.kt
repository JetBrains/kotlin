// WITH_STDLIB
// TARGET_BACKEND: WASM
// USE_OLD_EXCEPTION_HANDLING_PROPOSAL
// WASM_FAILS_IN: Wasmtime

import kotlin.test.*

fun box(): String {
    assertFailsWith<IllegalStateException>("My another error") {
        try {
            error("My error")
        } catch (e: Throwable) {
            error("My another error")
        }
    }
    return "OK"
}
