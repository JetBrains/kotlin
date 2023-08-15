// IGNORE_BACKEND: WASM

// FILE: test.kt

fun box() {
    "OK"
    fun bar() {
        "OK"
    }
    "OK"
    bar()
    "OK"
}

// EXPECTATIONS JVM JVM_IR
// test.kt:6 box
// EXPECTATIONS JVM
// test.kt:7 box
// EXPECTATIONS JVM JVM_IR
// test.kt:10 box
// test.kt:11 box
// EXPECTATIONS JVM
// test.kt:8 invoke
// test.kt:9 invoke
// EXPECTATIONS JVM_IR
// test.kt:8 box$bar
// test.kt:9 box$bar
// EXPECTATIONS JVM JVM_IR
// test.kt:12 box
// test.kt:13 box

// EXPECTATIONS JS_IR
// test.kt:11 box
// test.kt:9 box$bar
// test.kt:13 box
