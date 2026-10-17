import kotlin.test.assertEquals

var log = ""

inline fun bar() {
    try {
        log += "bar;"
    } finally {
        log += "barFinally;"
    }
}

inline fun foo(block: () -> Unit) {
    try {
        block()
    } finally {
        bar()
        log += "afterBar;"
    }
}

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
