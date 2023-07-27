
// FILE: test.kt
fun box() {
    for (i in 1..3) {
        foo(i)
    }
}

inline fun foo(n: Int) {}

// EXPECTATIONS JVM_IR
// test.kt:4 box
// test.kt:5 box
// test.kt:9 box
// test.kt:4 box
// test.kt:5 box
// test.kt:9 box
// test.kt:4 box
// test.kt:5 box
// test.kt:9 box
// test.kt:4 box
// test.kt:7 box

// EXPECTATIONS JS_IR
// test.kt:4 box
// test.kt:4 box
// test.kt:4 box
// test.kt:4 box
// test.kt:4 box
// test.kt:4 box
// test.kt:4 box
// test.kt:4 box
// test.kt:4 box
// test.kt:4 box
// test.kt:4 box
// test.kt:7 box

// EXPECTATIONS WASM
// test.kt:1 $box
// test.kt:4 $box (14, 4, 17, 4, 14, 9, 14, 4, 17, 4, 4, 14, 9, 14, 4, 17, 4, 4, 14, 9, 14, 4, 17, 4, 4)
// test.kt:5 $box (8, 12, 8, 12, 8, 12)
// test.kt:7 $box
