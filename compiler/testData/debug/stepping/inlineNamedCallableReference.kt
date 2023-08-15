// IGNORE_BACKEND: WASM
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
// test.kt:4 box
// test.kt:5 box
// test.kt:9 box
// test.kt:5 box
// test.kt:12 g
// test.kt:9 box
// test.kt:10 box
// test.kt:6 box

// EXPECTATIONS JS_IR
// test.kt:4 box
// test.kt:5 box
// test.kt:12 g
// test.kt:6 box