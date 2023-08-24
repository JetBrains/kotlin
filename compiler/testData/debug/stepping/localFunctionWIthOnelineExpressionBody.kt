// IGNORE_BACKEND: WASM

// FILE: test.kt

fun box() {
    "OK"
    fun bar() = "OK"
    "OK"
    bar()
    "OK"
}

// EXPECTATIONS JVM JVM_IR
// test.kt:6 box
// EXPECTATIONS JVM
// test.kt:7 box
// EXPECTATIONS JVM JVM_IR
// test.kt:8 box
// test.kt:9 box
// EXPECTATIONS JVM
// test.kt:7 invoke
// EXPECTATIONS JVM_IR
// test.kt:7 box$bar
// EXPECTATIONS JVM JVM_IR
// test.kt:9 box
// test.kt:10 box
// test.kt:11 box

// EXPECTATIONS JS_IR
// test.kt:9 box
// test.kt:7 box$bar
// test.kt:11 box
