// WITH_STDLIB

import kotlin.test.*

fun box(): String {
    foo(0)
    return "OK"
}

fun foo(x: Int) {
    when (x) {
        0 -> 0
    }
}
