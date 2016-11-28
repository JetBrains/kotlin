// !DIAGNOSTICS: -UNUSED_VARIABLE
package test

fun nullableFun(): Int? = null
fun Int.foo() {}

val test1 = <!RESERVED_SYNTAX_IN_CALLABLE_REFERENCE_LHS!>nullableFun()<!>?::<!UNSAFE_CALL!>foo<!>