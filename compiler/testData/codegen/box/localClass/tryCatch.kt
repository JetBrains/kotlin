// WITH_STDLIB

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
