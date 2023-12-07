// IGNORE_BACKEND: WASM
// FILE: test.kt

inline fun alsoInline() = "OK"

inline fun ifoo(s: String = alsoInline()): String {
    return s
}

fun box(): String {
    return ifoo()
}

// EXPECTATIONS JVM_IR
// test.kt:11 box
// test.kt:6 box
// test.kt:4 box
// test.kt:6 box
// test.kt:7 box
// test.kt:11 box

// EXPECTATIONS JS_IR
// test.kt:11 box
