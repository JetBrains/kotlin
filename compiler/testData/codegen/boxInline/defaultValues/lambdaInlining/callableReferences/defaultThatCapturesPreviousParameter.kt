// SKIP_INLINE_CHECK_IN: test$default
// FILE: 1.kt
package test

class Foo(val a: String) {
    fun bar() = a
}

inline fun test(x: String = "OK", block: () -> String = Foo(x)::bar): String {
    return block()
}

// FILE: 2.kt
import test.*

fun box(): String {
    return test()
}
