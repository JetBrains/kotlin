
// FILE: test.kt

fun box() {
    val x =
            42
}

// EXPECTATIONS JVM_IR
// test.kt:6 box
// test.kt:5 box
// test.kt:7 box

// EXPECTATIONS JS_IR
// test.kt:6 box
// test.kt:7 box

// EXPECTATIONS WASM
// test.kt:6 $box (12)
// test.kt:5 $box (4)
// test.kt:7 $box (1)
