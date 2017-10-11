package codegen.inline.inline25

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

@Test fun runTest() {
    bar {
        println(it)
    }

    baz {
        println(this)
    }
}
