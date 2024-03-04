// KT-33411
// IGNORE_BACKEND: JVM_IR

// MODULE: m1
// FILE: m1.kt
fun getOK() = "OK"

// MODULE: m2
// FILE: m2.kt
fun getOK() = "OK"

// MODULE: main(m1)(m2)
// FILE: main.kt

fun box() = getOK()
