// FILE: test.kt
fun box() {
    var x = false
    f {
        x = true
    }
}

fun f(block: () -> Unit) {
    block()
}

// LINENUMBERS
// test.kt:3 box
// test.kt:4 box
// test.kt:10 f
// test.kt:5 invoke
// test.kt:6 invoke
// test.kt:10 f
// test.kt:11 f
// test.kt:7 box
