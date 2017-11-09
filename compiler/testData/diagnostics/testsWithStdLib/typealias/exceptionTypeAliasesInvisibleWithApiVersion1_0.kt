// !LANGUAGE: +TypeAliases
// !API_VERSION: 1.0
// FILE: test.kt
val fooException = Exception("foo")
val barException = kotlin.<!UNRESOLVED_REFERENCE!>Exception<!>("bar")
