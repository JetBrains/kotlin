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
// test.kt:3
// test.kt:4
// test.kt:8
// test.kt:4
// test.kt:11
// test.kt:9
// test.kt:5
