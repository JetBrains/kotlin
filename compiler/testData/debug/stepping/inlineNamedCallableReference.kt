// FILE: test.kt
fun box() {
    var x = false
    f(::g)
}

inline fun f(block: () -> Unit) {
    block()
}

fun g() {}

// EXPECTATIONS JVM JVM_IR
// test.kt:3 box
// test.kt:4 box
// test.kt:8 box
// test.kt:4 box
// test.kt:11 g
// test.kt:8 box
// test.kt:9 box
// test.kt:5 box

// EXPECTATIONS JS_IR
// test.kt:3 box
// test.kt:4 box
// test.kt:11 g
// test.kt:5 box