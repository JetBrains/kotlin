// IGNORE_BACKEND_K2: WASM

// FILE: test.kt

fun box() {
    lookAtMe {
        42
    }
}

inline fun lookAtMe(f: () -> Int) {
    val a = 21
    a + f()
}

// EXPECTATIONS JVM_IR
// test.kt:6 box
// test.kt:12 box
// test.kt:13 box
// test.kt:7 box
// test.kt:13 box
// test.kt:14 box
// test.kt:9 box

// EXPECTATIONS JS_IR
// test.kt:12 box
// test.kt:13 box
// test.kt:9 box

// EXPECTATIONS WASM
// test.kt:1 $box
// test.kt:6 $box
// test.kt:12 $box
// test.kt:13 $box (4, 8, 4, 4)
// test.kt:7 $box (8, 8)
// test.kt:9 $box
