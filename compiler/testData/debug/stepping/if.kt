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

// LINENUMBERS
// test.kt:4 box
// test.kt:13 box
// test.kt:5 box
// test.kt:10 getA
// test.kt:5 box
// test.kt:7 box
// test.kt:13 box
// test.kt:7 box
