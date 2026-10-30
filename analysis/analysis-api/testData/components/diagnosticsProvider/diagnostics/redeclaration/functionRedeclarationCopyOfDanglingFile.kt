// KT-79200
// COPY_OF_DANGLING_FILE

// FILE: foo1.kt
fun foo(value: Int): String = ""

// FILE: foo2.kt
fun foo(value: String): String = ""

// FILE: foo3.kt
fun foo(value: Int): String = ""
