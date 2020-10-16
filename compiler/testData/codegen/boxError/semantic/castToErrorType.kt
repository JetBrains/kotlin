// ERROR_POLICY: SEMANTIC

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