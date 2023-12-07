// IGNORE_BACKEND: WASM

// FILE: test.kt

fun box() {
    "OK"
    fun bar() = "OK"
    "OK"
    bar()
    "OK"
}

// EXPECTATIONS JVM_IR
// test.kt:6 box
// test.kt:8 box
// test.kt:9 box
// test.kt:7 box$bar
// test.kt:9 box
// test.kt:10 box
// test.kt:11 box

// EXPECTATIONS JS_IR
// test.kt:9 box
// test.kt:7 box$bar
// test.kt:11 box
