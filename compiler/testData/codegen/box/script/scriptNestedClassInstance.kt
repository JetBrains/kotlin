// TARGET_BACKEND: JVM
// IGNORE_BACKEND_FIR: JVM_IR
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
