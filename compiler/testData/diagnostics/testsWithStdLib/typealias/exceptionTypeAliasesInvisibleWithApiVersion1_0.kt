// !LANGUAGE: +TypeAliases
// !API_VERSION: 1.0
// IGNORE_DIAGNOSTIC_API
// IGNORE_REVERSED_RESOLVE
// ^KT-60996
// FILE: test.kt
val fooException = Exception("foo")
val barException = kotlin.<!UNRESOLVED_REFERENCE!>Exception<!>("bar")
