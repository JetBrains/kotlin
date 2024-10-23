// TARGET_BACKEND: JVM
// IGNORE_BACKEND: JVM_IR
// IGNORE_BACKEND_K2: JS_IR, JS_IR_ES6
// FIR status: script declarations are not visible from other sources
// IGNORE_LIGHT_ANALYSIS
// WITH_STDLIB
// FILE: test.kt

fun box(): String =
    Script(emptyArray<String>()).x

// FILE: script.kts

val x = "OK"
