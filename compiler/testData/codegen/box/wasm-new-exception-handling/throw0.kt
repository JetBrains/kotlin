// WITH_STDLIB
// TARGET_BACKEND: WASM
// USE_NEW_EXCEPTION_HANDLING_PROPOSAL
// TODO: remove the test when KT-66906 will be resolved

import kotlin.test.*

fun box(): String {
    val cond = 1
    if (cond == 2) throw RuntimeException()
    if (cond == 3) throw NoSuchElementException("no such element")
    if (cond == 4) throw Error("error happens")

    return "OK"
}
