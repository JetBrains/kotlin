// WITH_STDLIB
// TARGET_BACKEND: WASM
// USE_OLD_EXCEPTION_HANDLING_PROPOSAL
// WASM_FAILS_IN: Wasmtime

import kotlin.test.*

fun box(): String {
    val cond = 1
    if (cond == 2) throw RuntimeException()
    if (cond == 3) throw NoSuchElementException("no such element")
    if (cond == 4) throw Error("error happens")

    return "OK"
}
