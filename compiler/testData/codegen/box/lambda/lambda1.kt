// WITH_STDLIB

import kotlin.test.*

val sb = StringBuilder()

fun box(): String {
    run {
        sb.append("OK")
    }
    return sb.toString()
}

fun run(f: () -> Unit) {
    f()
}
