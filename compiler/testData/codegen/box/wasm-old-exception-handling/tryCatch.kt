// WITH_STDLIB
// TARGET_BACKEND: WASM
// USE_OLD_EXCEPTION_HANDLING_PROPOSAL
// WASM_FAILS_IN: Wasmtime

import kotlin.test.*

val sb = StringBuilder()

private fun foo() {
    val local =
            object {
                fun bar() {
                    try {
                    } catch (t: Throwable) {
                        sb.appendLine(t)
                    }
                }
            }
    local.bar()
}

fun box(): String {
    sb.append("OK")
    foo()
    return sb.toString()
}