


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
// test.kt:10 $box (10)
// test.kt:11 $box (4)
// test.kt:6 $box (36)
// test.kt:7 $box (4)
// test.kt:12 $box (8, 12)
// test.kt:8 $box (1)
// test.kt:14 $box (1)
