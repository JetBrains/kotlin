// WITH_STDLIB
// TARGET_BACKEND: WASM
// USE_NEW_EXCEPTION_HANDLING_PROPOSAL
// WASM_FAILS_IN_SINGLE_MODULE_MODE
// TODO: remove the test when KT-66906 will be resolved

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