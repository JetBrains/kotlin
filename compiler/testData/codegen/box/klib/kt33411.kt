// KT-33411: Overload resolution ambiguity:
// public fun getOK(): String defined in root package
// public fun getOK(): String defined in root package
// IGNORE_BACKEND_K1: JVM, JVM_IR

// MODULE: m1
// FILE: m1.kt
fun getOK() = "OK"

// MODULE: m2
// FILE: m2.kt
fun getOK() = "OK"

// MODULE: main(m1)(m2)
// FILE: main.kt

fun box() = getOK()
