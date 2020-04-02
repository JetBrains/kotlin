//FILE: test.kt
fun box() {
    val a = 1
    val b = 2
    try {
        throwIfLess(a, b)
    } catch (e: java.lang.Exception) {
        throwIfLess(a, b)
    }
    throwIfLess(b,a)
}

fun throwIfLess(a: Int, b: Int) {
    if (a<b)
        throw java.lang.IllegalStateException()
}
// LINENUMBERS
// test.kt:3
// test.kt:4
// test.kt:5
// test.kt:6
// test.kt:14
// test.kt:15
// test.kt:7
// test.kt:8
// test.kt:14
// test.kt:15
