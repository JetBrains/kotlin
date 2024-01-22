// !DIAGNOSTICS: -UNUSED_VARIABLE
package test

fun nullableFun(): Int? = null
fun Int.foo() {}

val test1 = <!SAFE_CALLABLE_REFERENCE_CALL!>nullableFun()?::<!UNSAFE_CALL!>foo<!><!>
