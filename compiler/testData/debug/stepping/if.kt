// IGNORE_BACKEND: WASM

// FILE: test.kt
fun box(): Int {
    if (
        getB() ==
        getA())
        return 0
    return getB()
}

fun getA() = 3

inline fun getB(): Int {
    return 1
}

// EXPECTATIONS JVM JVM_IR
// test.kt:6 box
// test.kt:15 box
// test.kt:7 box
// test.kt:12 getA
// test.kt:7 box
// test.kt:9 box
// test.kt:15 box
// test.kt:9 box

// EXPECTATIONS JS_IR
// test.kt:5 box
// test.kt:12 getA
// test.kt:9 box
