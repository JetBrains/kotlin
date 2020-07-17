// FILE: test.kt
fun box() {
    var x = false
    f {
        x = true
    }
    var y =
        true
    f {
        y = false
    }
}

inline fun f(block: () -> Unit) {
    block()
}

// LINENUMBERS
// test.kt:3 box
// test.kt:4 box
// test.kt:15 box
// test.kt:5 box
// test.kt:6 box
// test.kt:16 box
// test.kt:7 box
// test.kt:8 box
// test.kt:7 box
// test.kt:9 box
// test.kt:15 box
// test.kt:10 box
// test.kt:11 box
// test.kt:16 box
// test.kt:12 box