// WITH_STDLIB

import kotlin.test.*

val sb = StringBuilder()

fun box(): String {
    apply("OK") {
        sb.append(this)
    }
    return sb.toString()
}

fun apply(str: String, block: String.() -> Unit) {
    str.block()
}
