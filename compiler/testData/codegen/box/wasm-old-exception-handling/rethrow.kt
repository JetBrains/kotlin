// WITH_STDLIB
// TARGET_BACKEND: WASM_JS
// USE_OLD_EXCEPTION_HANDLING_PROPOSAL

import kotlin.test.*

fun box(): String {
    assertFailsWith<IllegalStateException>("My error") {
        try {
            error("My error")
        } catch (e: Throwable) {
            throw e
        }
    }
    return "OK"
}
