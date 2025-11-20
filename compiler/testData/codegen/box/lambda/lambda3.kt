// WITH_STDLIB

import kotlin.test.*

val sb = StringBuilder()

fun box(): String {
    var str = "OK"
    run {
        sb.append(str)
    }
    return sb.toString()
}

fun run(f: () -> Unit) {
    f()
}
