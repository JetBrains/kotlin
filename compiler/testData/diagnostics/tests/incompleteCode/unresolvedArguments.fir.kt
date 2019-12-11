// !WITH_NEW_INFERENCE
//!DIAGNOSTICS: -UNUSED_PARAMETER

fun <T> foo(i: Int, t: T) {}
fun <T> foo(s: String, t: T) {}

fun bar(i: Int) {}
fun bar(s: String) {}

fun test() {
    <!AMBIGUITY!>foo<!>(<!UNRESOLVED_REFERENCE!>rrr<!>, 1)
    <!AMBIGUITY!>bar<!>(<!UNRESOLVED_REFERENCE!>rrr<!>)
}
