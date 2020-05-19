// FILE: test.kt
fun box() {
    var x = false
    f(::g)
}

fun f(block: () -> Unit) {
    block()
}

fun g() {}

// LINENUMBERS
// test.kt:3 box
// test.kt:4 box
// test.kt:8 f
// test.kt:11 g
// test.kt:4 invoke
// test.kt:8 f
// test.kt:9 f
// test.kt:5 box
