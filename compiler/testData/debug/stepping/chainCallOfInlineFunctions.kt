
// FILE: test.kt
inline fun foo1() = "OK"

inline fun foo2() =
    foo1()

inline fun foo3() =
    foo2()

inline fun foo4() =
    foo3()

inline fun foo5() =
    foo4()

fun box() {
    foo5()
}

// EXPECTATIONS JVM_IR
// test.kt:18 box
// test.kt:15 box
// test.kt:12 box
// test.kt:9 box
// test.kt:6 box
// test.kt:3 box
// test.kt:6 box
// test.kt:9 box
// test.kt:12 box
// test.kt:15 box
// test.kt:19 box

// EXPECTATIONS NATIVE
// test.kt:18 box
// test.kt:15 box
// test.kt:12 box
// test.kt:9 box
// test.kt:6 box
// test.kt:3 box
// test.kt:6 box
// test.kt:9 box
// test.kt:12 box
// test.kt:15 box
// test.kt:19 box

// EXPECTATIONS JS_IR
// test.kt:19 box

// EXPECTATIONS WASM
// test.kt:18 $box (4)
// test.kt:15 $box (4)
// test.kt:12 $box (4)
// test.kt:9 $box (4)
// test.kt:6 $box (4)
// test.kt:3 $box (20, 24)
// test.kt:6 $box (10)
// test.kt:9 $box (10)
// test.kt:12 $box (10)
// test.kt:15 $box (10)
// test.kt:19 $box (1)
