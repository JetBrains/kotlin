// !LANGUAGE: -TypeAliases
// FILE: test.kt
val fooException = Exception("foo")
val barException = kotlin.<!UNRESOLVED_REFERENCE!>Exception<!>("bar")
