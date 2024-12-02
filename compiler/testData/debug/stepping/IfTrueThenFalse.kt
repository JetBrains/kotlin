
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
// test.kt:5 $box (10)
// test.kt:6 $box (8)
// test.kt:3 $cond (13)
// test.kt:9 $box (9)
// test.kt:10 $box (1)
