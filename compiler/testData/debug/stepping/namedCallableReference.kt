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

// LINENUMBERS
// test.kt:3 box
// test.kt:4 box
// test.kt:8 f
// LINENUMBERS JVM_IR
// test.kt:4 invoke
// LINENUMBERS
// test.kt:11 g
// test.kt:4 invoke
// test.kt:8 f
// test.kt:9 f
// test.kt:5 box
