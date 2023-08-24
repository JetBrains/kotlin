// IGNORE_BACKEND: WASM
// FILE: test.kt

fun box() {
    val x =
            42
}

// EXPECTATIONS JVM JVM_IR
// test.kt:6 box
// test.kt:5 box
// test.kt:7 box

// EXPECTATIONS JS_IR
// test.kt:6 box
// test.kt:7 box