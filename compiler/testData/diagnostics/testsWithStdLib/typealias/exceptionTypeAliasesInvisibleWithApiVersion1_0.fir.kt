// LANGUAGE: +TypeAliases
// API_VERSION: 1.0
// FILE: test.kt
val fooException = Exception("foo")
val fooException2 = java.lang.Exception("foo")
val barException = kotlin.<!NONE_APPLICABLE!>Exception<!>("bar")

fun f(e: Exception, e2: java.lang.Exception, e3: kotlin.<!API_NOT_AVAILABLE!>Exception<!>) {
}