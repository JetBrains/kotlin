// IGNORE_BACKEND_K2: WASM
// FILE: test.kt
fun cond() = false

fun box() {
    if (cond())
        cond()
    else
         false
}

// EXPECTATIONS JVM_IR
// test.kt:6 box
// test.kt:3 cond
// test.kt:6 box
// test.kt:9 box
// test.kt:10 box

// EXPECTATIONS JS_IR
// test.kt:6 box
// test.kt:3 cond
// EXPECTATIONS FIR JS_IR
// test.kt:9 box
// EXPECTATIONS JS_IR
// test.kt:10 box

// EXPECTATIONS WASM
// test.kt:1 $box
// test.kt:6 $box
// test.kt:3 $cond (13, 18)
// test.kt:10 $box
