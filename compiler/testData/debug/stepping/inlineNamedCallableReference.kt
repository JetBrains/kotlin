// FILE: test.kt
fun box() {
    var x = false
    f(::g)
}

inline fun f(block: () -> Unit) {
    block()
}

fun g() {}

// LINENUMBERS
// test.kt:3 box
// test.kt:4 box
// test.kt:8 box
// test.kt:4 box
// test.kt:11 g
// test.kt:9 box
// test.kt:5 box
