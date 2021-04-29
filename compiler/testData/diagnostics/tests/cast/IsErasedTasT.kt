// FIR_IDENTICAL
fun <T> testing(a: T) = <!USELESS_IS_CHECK!>a is T<!>
