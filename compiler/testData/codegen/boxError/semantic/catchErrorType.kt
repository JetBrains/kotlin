// IGNORE_ERRORS
// ERROR_POLICY: SEMANTIC
// IGNORE_BACKEND_K2: JS_IR

// MODULE: lib
// FILE: t.kt

fun bar() { throw Exception("..") }

fun foo(): String {
    try {
        bar()
    } catch (e: ErrType) {
        throw Expception(e)
    }
}

// MODULE: main(lib)
// FILE: b.kt

fun box(): String {
    try {
        foo()
    } catch (e: IllegalStateException) {
        return "OK"
    }
    return "fail"
}