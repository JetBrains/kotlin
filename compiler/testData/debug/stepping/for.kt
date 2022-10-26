// FILE: test.kt
fun box() {
    for (i in 1..3) {
        foo(i)
    }
}

inline fun foo(n: Int) {}

// EXPECTATIONS JVM JVM_IR
// test.kt:3 box
// test.kt:4 box
// test.kt:8 box
// test.kt:3 box
// test.kt:4 box
// test.kt:8 box
// test.kt:3 box
// test.kt:4 box
// test.kt:8 box
// test.kt:3 box
// test.kt:6 box

// EXPECTATIONS JS_IR
// test.kt:3 box
// test.kt:3 box
// test.kt:3 box
// test.kt:3 box
// test.kt:3 box
// test.kt:3 box
// test.kt:3 box
// test.kt:3 box
// test.kt:3 box
// test.kt:3 box
// test.kt:3 box
// test.kt:6 box
