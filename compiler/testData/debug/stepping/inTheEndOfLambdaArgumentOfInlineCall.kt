// IGNORE_BACKEND: JS_IR
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

// EXPECTATIONS JVM JVM_IR
// test.kt:5 box
// test.kt:12 box
// test.kt:20 nop
// test.kt:13 box
// test.kt:6 box
// test.kt:20 nop
// test.kt:7 box
// test.kt:17 box
// test.kt:20 nop
// test.kt:18 box
// test.kt:8 box
// test.kt:13 box
// test.kt:14 box
// test.kt:9 box

// EXPECTATIONS JS_IR
