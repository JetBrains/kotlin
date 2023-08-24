// IGNORE_BACKEND: WASM
// IGNORE_BACKEND_K2: JVM_IR

// FILE: test.kt
fun box(): String {
    val
            o
            =
        "O"


    val k = "K"

    return o + k
}

// EXPECTATIONS JVM JVM_IR

// test.kt:9 box
// test.kt:7 box
// test.kt:12 box
// test.kt:14 box

// EXPECTATIONS JS_IR
// test.kt:9 box
// test.kt:12 box
// test.kt:14 box
