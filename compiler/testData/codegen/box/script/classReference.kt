// TARGET_BACKEND: JVM
// IGNORE_BACKEND_K2: JVM_IR, JS, JS_ES6
// FIR status: script declarations are not visible from other sources
// IGNORE_LIGHT_ANALYSIS
// WITH_STDLIB
// FILE: test.kt

fun box(): String =
    OK::class.java.simpleName

// FILE: OK.kts
