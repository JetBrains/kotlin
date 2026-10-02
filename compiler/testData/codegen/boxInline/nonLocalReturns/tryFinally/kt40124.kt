import kotlin.test.assertEquals

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

fun runFoo(): String {
    foo { log += "foo;"; return "OK" }
    return "fail"
}

fun box(): String {
    assertEquals("OK", runFoo())
    assertEquals("foo;finally;", log)
    return "OK"
}

// Check that the alwaysTrue marker is optimized out of the inlined bytecode (otherwise the count would be 2).
//
// CHECK_BYTECODE_TEXT
// 1 kotlin/jvm/internal/InlineMarker.alwaysTrue
