// TARGET_BACKEND: JVM
// IGNORE_BACKEND_FIR: JVM_IR
// FIR status: scripts aren't supported yet
// IGNORE_LIGHT_ANALYSIS
// WITH_STDLIB
// FILE: test.kt

fun box(): String =
    Script(emptyArray<String>()).x

// FILE: script.kts

val x = "OK"
