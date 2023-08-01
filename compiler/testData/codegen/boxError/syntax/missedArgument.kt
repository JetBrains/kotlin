// IGNORE_ERRORS
// ERROR_POLICY: SYNTAX
// IGNORE_BACKEND_K2: JS_IR, JS_IR_ES6

// MODULE: lib
// FILE: t.kt

1sdasf

fun bar() {}

fun foo() { bar(,,,,,,,) }

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