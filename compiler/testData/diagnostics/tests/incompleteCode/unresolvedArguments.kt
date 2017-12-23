// !WITH_NEW_INFERENCE
//!DIAGNOSTICS: -UNUSED_PARAMETER

fun <T> foo(i: Int, t: T) {}
fun <T> foo(s: String, t: T) {}

fun bar(i: Int) {}
fun bar(s: String) {}

fun test() {
    <!NI;OVERLOAD_RESOLUTION_AMBIGUITY!>foo<!>(<!UNRESOLVED_REFERENCE!>rrr<!>, 1)
    <!NI;OVERLOAD_RESOLUTION_AMBIGUITY!>bar<!>(<!UNRESOLVED_REFERENCE!>rrr<!>)
}
