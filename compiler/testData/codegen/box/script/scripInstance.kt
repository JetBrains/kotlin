// TARGET_BACKEND: JVM
// IGNORE_BACKEND_K2: JVM_IR, JS_IR, JS_IR_ES6
// FIR status: scripts aren't supported yet
// IGNORE_LIGHT_ANALYSIS
// WITH_STDLIB
// FILE: test.kt

fun box(): String =
    Script(emptyArray<String>()).x

// FILE: script.kts

val x = "OK"
