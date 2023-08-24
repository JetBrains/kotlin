// IGNORE_BACKEND: WASM
// FILE: test.kt
inline fun foo(stringMaker: () -> String = { "OK" }): String {
    return stringMaker()
}

inline fun foo2(stringMaker: () -> String = {
    "OK"
    // Comment
}): String {
    return stringMaker()
}

fun box(): String {
    foo()
    foo2()
    return "OK"
}

// EXPECTATIONS JVM JVM_IR
// test.kt:15 box
// test.kt:3 box
// test.kt:4 box
// test.kt:3 box
// test.kt:4 box
// test.kt:16 box
// test.kt:7 box
// test.kt:11 box
// test.kt:8 box
// test.kt:11 box
// test.kt:17 box

// EXPECTATIONS JS_IR
// test.kt:17 box
