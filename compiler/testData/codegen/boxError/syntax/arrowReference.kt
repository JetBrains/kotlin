// IGNORE_ERRORS
// ERROR_POLICY: SYNTAX
// IGNORE_BACKEND_K2: JS_IR

// MODULE: lib
// FILE: t.kt

fun foo() { this->bar() }

// MODULE: main(lib)
// FILE: b.kt

fun box(): String {
    try {
        foo()
    } catch (e: IllegalStateException) {
        return "OK"
    }
    return "FAIL"
}