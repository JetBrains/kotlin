// KT-66338: K2/JVM_IR should also raise an error `OVERLOAD_RESOLUTION_AMBIGUITY`, like K1/JVM_IR does
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
