// FILE: test.kt
fun box() {
    var x = false
    f {
        x = true
    }
}

inline fun f(block: () -> Unit) {
    block()
}

// LINENUMBERS
// test.kt:3 box
// test.kt:4 box
// test.kt:10 box
// test.kt:5 box
// test.kt:6 box
// test.kt:11 box
// test.kt:7 box
