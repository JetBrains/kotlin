// TARGET_BACKEND: JVM
// IGNORE_BACKEND_K2: JVM_IR, JS_IR
// FIR status: scripts aren't supported yet
// IGNORE_LIGHT_ANALYSIS
// WITH_STDLIB
// FILE: test.kt

fun box(): String =
    Script.Nested().x

// FILE: script.kts

class Nested {
    val x = "OK"
}
