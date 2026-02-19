// WITH_STDLIB
// TARGET_BACKEND: WASM
// USE_NEW_EXCEPTION_HANDLING_PROPOSAL
// TODO: remove the test when KT-66906 will be resolved

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