// TARGET_BACKEND: JVM
// IGNORE_BACKEND_K1: JVM, JVM_IR, JS_IR, JS_IR_ES6
// IGNORE_LIGHT_ANALYSIS
// WITH_STDLIB
// FILE: test.kt

fun box(): String =
    Nested().x

// FILE: script.kts

class Nested {
    val x = "OK"
}
