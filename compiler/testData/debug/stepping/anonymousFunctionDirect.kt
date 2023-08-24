// IGNORE_BACKEND: WASM
// FILE: test.kt

fun box() {
    {
        "OK"
    }()
}

// EXPECTATIONS JVM_IR
// test.kt:6 box
// EXPECTATIONS JVM
// test.kt:5 box
// test.kt:6 invoke
// test.kt:5 box
// EXPECTATIONS JVM JVM_IR
// test.kt:8 box

// EXPECTATIONS JS_IR
// test.kt:5 box
// test.kt:6 box$lambda
// test.kt:8 box