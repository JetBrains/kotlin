
// FILE: test.kt
fun box() {
    var x = false
    f(::g)
}

fun f(block: () -> Unit) {
    block()
}

fun g() {}

// EXPECTATIONS JVM_IR
// test.kt:4 box
// test.kt:5 box
// test.kt:9 f
// test.kt:5 invoke
// test.kt:12 g
// test.kt:5 invoke
// test.kt:9 f
// test.kt:10 f
// test.kt:6 box

// EXPECTATIONS JS_IR
// test.kt:4 box
// test.kt:5 box
// test.kt:5 g$ref
// test.kt:5 box
// test.kt:9 f
// test.kt:12 g
// test.kt:10 f
// test.kt:6 box

// EXPECTATIONS WASM
// test.kt:4 $box (12, 4)
// test.kt:5 $box
// test.kt:9 $f (4, 4, 4)
// test.kt:12 $g
// test.kt:10 $f
// test.kt:6 $box
