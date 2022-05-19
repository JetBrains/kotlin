// IGNORE_BACKEND: JS_IR
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

// EXPECTATIONS
// test.kt:5 box
// test.kt:14 box
// test.kt:6 box
// test.kt:11 getA
// test.kt:6 box
// test.kt:8 box
// test.kt:14 box
// test.kt:8 box
