// WITH_STDLIB

import kotlin.test.*

val sb = StringBuilder()

fun box(): String {
    lateinit var s: String

    fun foo() = s

    try {
        sb.appendLine(foo())
    }
    catch (e: RuntimeException) {
        sb.append("OK")
        return sb.toString()
    }
    return "Fail"
}
