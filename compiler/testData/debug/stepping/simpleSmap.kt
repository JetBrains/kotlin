// IGNORE_BACKEND: JS_IR

// FILE: test.kt

inline fun inlineFun(s: () -> Unit) {
    s()
}

fun box() {
    inlineFun {
        "OK"
    }
}

// EXPECTATIONS
// test.kt:10 box
// test.kt:6 box
// test.kt:11 box
// test.kt:12 box
// test.kt:6 box
// test.kt:7 box
// test.kt:13 box
