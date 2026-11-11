// MODULE: a
// FILE: a.kt

var log = ""

inline fun foo(block: () -> Unit) {
    try {
        while (true)
            block()
    } finally {
        log += "finally;"
    }
    log += "unreachable;"
}

// MODULE: b(a)
// FILE: test.kt

import kotlin.test.assertEquals

fun runFoo(): String {
    foo { log += "foo;"; return "OK" }
    return "fail"
}

fun box(): String {
    assertEquals("OK", runFoo())
    assertEquals("foo;finally;", log)
    return "OK"
}
