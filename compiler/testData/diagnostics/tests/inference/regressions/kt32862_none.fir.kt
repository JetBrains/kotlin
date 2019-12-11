// !DIAGNOSTICS: -UNUSED_PARAMETER
// !WITH_NEW_INFERENCE

fun foo(s: String) {}
fun foo(i: Long) {}

fun bar(f: (Boolean) -> Unit) {}

fun test() {
    <!INAPPLICABLE_CANDIDATE!>bar<!>(<!UNRESOLVED_REFERENCE!>::foo<!>)
}
