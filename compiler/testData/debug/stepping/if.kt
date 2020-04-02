//FILE: test.kt
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
// test.kt:4
// test.kt:13
// test.kt:5
// test.kt:10
// test.kt:5
// test.kt:7
// test.kt:13
// test.kt:7
