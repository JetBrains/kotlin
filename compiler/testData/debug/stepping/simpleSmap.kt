


// FILE: test.kt

inline fun inlineFun(s: () -> Unit) {
    s()
}

fun box() {
    inlineFun {
        "OK"
    }
}

// EXPECTATIONS JVM_IR
// test.kt:11 box
// test.kt:7 box
// test.kt:12 box
// test.kt:13 box
// test.kt:7 box
// test.kt:8 box
// test.kt:14 box

// EXPECTATIONS JS_IR
// test.kt:14 box

// EXPECTATIONS WASM
// test.kt:11 $box
// test.kt:7 $box
// test.kt:12 $box (8, 8, 8)
// test.kt:14 $box
