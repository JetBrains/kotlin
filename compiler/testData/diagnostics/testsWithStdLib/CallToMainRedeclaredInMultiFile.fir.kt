// !LANGUAGE: +WarningOnMainUnusedParameter
// !DIAGNOSTICS: +UNUSED_PARAMETER

// FILE: a.kt
fun main(args: Array<String>) {}

// FILE: b.kt
fun main(args: Array<String>) {}

// FILE: c.kt
fun foo() { main(arrayOf("a", "b")) }
