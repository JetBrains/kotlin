// FIR_IDENTICAL
// DIAGNOSTICS: -DEBUG_INFO_MISSING_UNRESOLVED
interface I
fun <E> foo() where E: I {}
fun <E> fooE1() where <!SYNTAX!><!>: I {}
fun <E> fooE2() where E: <!SYNTAX!><!>{}
fun <E> fooE3() where <!SYNTAX!><!>{}
