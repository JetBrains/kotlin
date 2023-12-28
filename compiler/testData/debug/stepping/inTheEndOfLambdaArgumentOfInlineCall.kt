

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

// EXPECTATIONS JVM_IR
// test.kt:6 box
// test.kt:13 box
// test.kt:21 nop
// test.kt:14 box
// test.kt:7 box
// test.kt:21 nop
// test.kt:8 box
// test.kt:18 box
// test.kt:21 nop
// test.kt:19 box
// test.kt:9 box
// test.kt:14 box
// test.kt:15 box
// test.kt:10 box

// EXPECTATIONS JS_IR
// test.kt:13 box
// test.kt:21 nop
// test.kt:7 box
// test.kt:21 nop
// test.kt:18 box
// test.kt:21 nop
// test.kt:10 box

// EXPECTATIONS WASM
// test.kt:1 $box
// test.kt:6 $box
// test.kt:13 $box
// test.kt:21 $nop (12, 12, 12)
// test.kt:14 $box
// test.kt:7 $box
// test.kt:8 $box
// test.kt:18 $box
// test.kt:10 $box
