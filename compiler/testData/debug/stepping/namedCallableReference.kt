// IGNORE_BACKEND: WASM
// FILE: test.kt
fun box() {
    var x = false
    f(::g)
}

fun f(block: () -> Unit) {
    block()
}

fun g() {}

// The synthetic invoke bridge method generated for in the callable reference has line numbers
// in the JVM_IR backend (as all bridges). In the JVM backend, only some bridges have line numbers.
// For some reason, when the bridge does not have line numbers, there is no method entry event
// for the invoke method bridged to. Therefore, the entry line number for invoke only shows
// up for JVM_IR.

// EXPECTATIONS JVM JVM_IR
// test.kt:4 box
// test.kt:5 box
// test.kt:9 f
// EXPECTATIONS JVM_IR
// test.kt:5 invoke
// EXPECTATIONS JVM JVM_IR
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
