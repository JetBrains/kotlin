
// FILE: test.kt
fun box() {
    val a = 1
    val b = 2
    try {
        throwIfLess(a, b)
    } catch (e: Exception) {
        throwIfLess(a, b)
    }
    throwIfLess(b,a)
}

fun throwIfLess(a: Int, b: Int) {
    if (a<b)
        throw IllegalStateException()
}

// EXPECTATIONS JVM_IR
// test.kt:4 box
// test.kt:5 box
// test.kt:6 box
// test.kt:7 box
// test.kt:15 throwIfLess
// test.kt:16 throwIfLess
// test.kt:8 box
// test.kt:9 box
// test.kt:15 throwIfLess
// test.kt:16 throwIfLess

// EXPECTATIONS JS_IR
// test.kt:4 box
// test.kt:5 box
// test.kt:7 box
// test.kt:15 throwIfLess
// test.kt:16 throwIfLess
// test.kt:8 box
// test.kt:8 box
// test.kt:9 box
// test.kt:15 throwIfLess
// test.kt:16 throwIfLess

// EXPECTATIONS WASM
// test.kt:3 $box (10)
// test.kt:4 $box (12)
// test.kt:5 $box (12)
// test.kt:7 $box (20, 23, 8)
// test.kt:14 $throwIfLess (32)
// test.kt:15 $throwIfLess (8, 10)
// test.kt:16 $throwIfLess (14, 8)
// test.kt:6 $box (4)
// test.kt:8 $box (27)
// test.kt:9 $box (20, 23, 8)
// test.kt:11 $box (4)
