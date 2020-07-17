// FILE: test.kt

inline fun inlineFun(s: () -> Unit) {
    s()
}

fun box() {
    inlineFun {
        "OK"
    }
}

// LINENUMBERS
// test.kt:8 box
// test.kt:4 box
// test.kt:9 box
// test.kt:10 box
// test.kt:5 box
// test.kt:11 box
