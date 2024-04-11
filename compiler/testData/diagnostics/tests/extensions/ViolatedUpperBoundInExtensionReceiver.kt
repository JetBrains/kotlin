// FIR_IDENTICAL
// LANGUAGE: +ContextReceivers
interface I<T: Number>

fun I<<!UPPER_BOUND_VIOLATED!>String<!>>.foo() {}

context(I<<!UPPER_BOUND_VIOLATED!>String<!>>)
fun bar() {}
