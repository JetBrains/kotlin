// ERROR_POLICY: SYNTAX

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