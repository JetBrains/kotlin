// IGNORE_ERRORS
// ERROR_POLICY: SEMANTIC
// IGNORE_BACKEND_K2: JS_IR

// MODULE: lib
// FILE: t.kt

fun foo(o: Any): Any = o as ErrType

// MODULE: main(lib)
// FILE: b.kt

fun box(): String {
    try {
        foo(Any())
    } catch (e: IllegalStateException) {
        return "OK"
    }
    return "fail"
}