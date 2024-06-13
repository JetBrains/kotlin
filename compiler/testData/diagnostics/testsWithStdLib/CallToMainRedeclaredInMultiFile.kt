// LANGUAGE: +WarningOnMainUnusedParameter
// DIAGNOSTICS: +UNUSED_PARAMETER

// FILE: a.kt
fun main(<!UNUSED_PARAMETER!>args<!>: Array<String>) {}

// FILE: b.kt
fun main(<!UNUSED_PARAMETER!>args<!>: Array<String>) {}

// FILE: c.kt
fun foo() { <!OVERLOAD_RESOLUTION_AMBIGUITY!>main<!>(arrayOf("a", "b")) }
