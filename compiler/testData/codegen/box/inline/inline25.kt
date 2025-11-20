// WITH_STDLIB

import kotlin.test.*

inline fun foo(block: String.() -> Unit) {
    "Ok".block()
}

inline fun bar(block: (String) -> Unit) {
    foo(block)
}

inline fun baz(block: String.() -> Unit) {
    block("Ok")
}

fun box(): String {
    bar {
        assertEquals("Ok", it)
    }

    baz {
        assertEquals("Ok", this)
    }

    return "OK"
}
