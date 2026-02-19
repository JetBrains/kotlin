// WITH_STDLIB

import kotlin.test.*

class Outer {
    inner class Inner {
        fun box() = "OK"
    }
}

fun box() = Outer().Inner().box()
