// FILE: test.kt
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
// test.kt:3 box
// test.kt:4 box
// test.kt:5 box
// test.kt:6 box
// test.kt:14 throwIfLess
// test.kt:15 throwIfLess
// test.kt:7 box
// test.kt:8 box
// test.kt:14 throwIfLess
// test.kt:15 throwIfLess
