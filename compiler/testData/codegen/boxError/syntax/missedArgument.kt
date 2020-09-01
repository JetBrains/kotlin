// ERROR_POLICY: SYNTAX

// FILE: t.kt

1sdasf

fun bar() {}

fun foo() { bar(,,,,,,,) }

// FILE: b.kt

fun box(): String {
    try {
        foo()
    } catch (e: IllegalStateException) {
        return "OK"
    }
    return "FAIL"
}