// WITH_STDLIB

import kotlin.test.*

val sb = StringBuilder()

inline fun foo(x: Any) {
    sb.append(if (x === x) "OK" else "FAIL")
}

fun box(): String {
    foo { 42 }

    return sb.toString()
}
