// IGNORE_BACKEND_FIR: JVM_IR
// FILE: 1.kt
// File names are important! This file should come before the other one
// in a lexicographic order.
const val x = "OK"

// FILE: 2.kt
fun box() = x
