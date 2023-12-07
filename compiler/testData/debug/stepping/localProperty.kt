// IGNORE_BACKEND: WASM


// FILE: test.kt
fun box(): String {
    val
            o
            =
        "O"


    val k = "K"

    return o + k
}

// EXPECTATIONS JVM_IR
// test.kt:9 box
// EXPECTATIONS ClassicFrontend JVM_IR
// test.kt:7 box
// EXPECTATIONS FIR JVM_IR
// test.kt:6 box
// EXPECTATIONS JVM_IR
// test.kt:12 box
// test.kt:14 box

// EXPECTATIONS JS_IR
// test.kt:9 box
// test.kt:12 box
// test.kt:14 box
