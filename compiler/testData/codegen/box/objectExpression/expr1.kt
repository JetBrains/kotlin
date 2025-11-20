// WITH_STDLIB

import kotlin.test.*

fun box(): String {
    val a = "a"

    val x = object {
        override fun toString(): String {
            return foo(a) + foo("b")
        }

        fun foo(s: String) = s + s
    }

    assertEquals("aabb", x.toString())
    return "OK"
}
