// ERROR_POLICY: SEMANTIC

// FILE: t.kt


fun foo() { bar() }

// FILE: b.kt

fun box(): String {
    try {
        foo()
    } catch (e: IllegalStateException) {
        return "OK"
    }
    return "FAIL"
}