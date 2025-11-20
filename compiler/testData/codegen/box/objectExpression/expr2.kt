// WITH_STDLIB

import kotlin.test.*

fun box(): String {
    val a = "OK"

    val x = object {
        override fun toString(): String {
            return foo {
                a
            }
        }

        fun foo(lambda: () -> String) = lambda()
    }

    return x.toString()
}
