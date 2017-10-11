package codegen.inline.inline7

import kotlin.test.*

@Suppress("NOTHING_TO_INLINE")
inline fun foo(vararg args: Int) {
    for (a in args) {
        println(a.toString())
    }
}

fun bar() {
    foo(1, 2, 3)
}

@Test fun runTest() {
    bar()
}
