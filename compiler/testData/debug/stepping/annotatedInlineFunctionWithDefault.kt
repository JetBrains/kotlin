// FILE: test.kt

annotation class Anno

@Anno
inline fun f(s: String = "OK"): String =
    s

fun box(): String =
    f()

// EXPECTATIONS JVM_IR
// test.kt:10 box
// test.kt:6 box
// test.kt:7 box
// test.kt:10 box

// EXPECTATIONS NATIVE
// test.kt:10 box
// test.kt:6 box
// test.kt:7 box
// test.kt:10 box

// EXPECTATIONS JS_IR
// test.kt:10 box

// EXPECTATIONS WASM
// test.kt:10 $box (4)
// test.kt:6 $box (25)
// test.kt:7 $box (4, 5)
// test.kt:10 $box (7)
