// IGNORE_DANGLING_FILES
// KT-79200

// SUPPRESS_INDIVIDUAL_DIAGNOSTICS_CHECK

// FILE: foo1.kt
fun foo(value: Int): String = ""

// FILE: foo2.kt
fun foo(value: String): String = ""

// FILE: foo3.kt
fun foo(value: Int): String = ""

// FILE: foo4.kt
fun foo(): Int = 0

// FILE: foo5.kt
fun foo(value: Int): Int = value + 1
