// FILE: 1.kt

var log = ""

inline fun bar() {
    try {
        log += "bar;"
    } finally {
        log += "barFinally;"
    }
}

// FILE: 2.kt

inline fun foo(block: () -> Unit) {
    try {
        block()
    } finally {
        bar()
        log += "afterBar;"
    }
}

// FILE: 3.kt

import kotlin.test.assertEquals

fun runFoo(): String {
    foo {
        log += "foo;"
        return "OK"
    }
    return "unreachable"
}

fun box(): String {
    val result = runFoo()
    assertEquals("foo;bar;barFinally;afterBar;", log)
    return result
}
