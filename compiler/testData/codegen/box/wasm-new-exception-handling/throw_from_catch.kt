// WITH_STDLIB
// TARGET_BACKEND: WASM
// USE_NEW_EXCEPTION_HANDLING_PROPOSAL
// TODO: remove the test when KT-66906 will be resolved

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
