// FILE: 1.kt

var log = ""

inline fun foo(block: () -> Unit) {
    try {
        block()
    } finally {
        run {
            try {
                log += "bar;"
            } finally {
                log += "barFinally;"
            }
        }
        log += "afterBar;"
    }
}

// FILE: 2.kt

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
