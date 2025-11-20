// WITH_STDLIB

import kotlin.test.*

class Outer(val s: String) {
    inner class Inner {
        fun box() = s
    }
}

fun box() = Outer("OK").Inner().box()
