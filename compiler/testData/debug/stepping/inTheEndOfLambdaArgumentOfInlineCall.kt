// FILE: test.kt

fun box() {
    bar {
        nop()
        baz()
    }
}

inline fun bar(f: () -> Unit) {
    nop()
    f()
}

inline fun baz() {
    nop()
}

fun nop() {}

// LINENUMBERS
// test.kt:4 box
// test.kt:11 box
// test.kt:19 nop
// test.kt:12 box
// test.kt:5 box
// test.kt:19 nop
// test.kt:6 box
// test.kt:16 box
// test.kt:19 nop
// test.kt:17 box
// test.kt:7 box
// test.kt:13 box
// test.kt:8 box
