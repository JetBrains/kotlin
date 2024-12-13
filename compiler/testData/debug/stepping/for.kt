
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
// test.kt:4 $box (14, 17)
// test.kt:5 $box (12, 8)
// test.kt:9 $box (25)
// test.kt:5 $box (8)
// test.kt:4 $box (17)
// test.kt:5 $box (12, 8)
// test.kt:9 $box (25)
// test.kt:5 $box (8)
// test.kt:4 $box (17)
// test.kt:5 $box (12, 8)
// test.kt:9 $box (25)
// test.kt:5 $box (8)
// test.kt:4 $box (17)
// test.kt:7 $box (1)
