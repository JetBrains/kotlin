// ERROR_POLICY: SEMANTIC

// FILE: t.kt

fun bar() { throw Exception("..") }

fun foo(): String {
    try {
        bar()
    } catch (e: ErrType) {
        throw Expception(e)
    }
}

// FILE: b.kt

fun box(): String {
    try {
        foo()
    } catch (e: IllegalStateException) {
        return "OK"
    }
    return "fail"
}