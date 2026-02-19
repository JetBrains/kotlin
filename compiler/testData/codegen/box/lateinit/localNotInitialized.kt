// WITH_STDLIB

import kotlin.test.*

val sb = StringBuilder()

fun box(): String {
    lateinit var s: String

    try {
        sb.appendLine(s)
    }
    catch (e: RuntimeException) {
        sb.append("OK")
        return sb.toString()
    }
    return "Fail"
}
