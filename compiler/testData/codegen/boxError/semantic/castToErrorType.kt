// ERROR_POLICY: SEMANTIC

// FILE: t.kt

fun foo(o: Any): Any = o as ErrType

// FILE: b.kt

fun box(): String {
    try {
        foo(Any())
    } catch (e: IllegalStateException) {
        return "OK"
    }
    return "fail"
}