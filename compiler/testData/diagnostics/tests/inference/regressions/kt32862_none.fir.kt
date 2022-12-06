// !DIAGNOSTICS: -UNUSED_PARAMETER

fun foo(s: String) {}
fun foo(i: Long) {}

fun bar(f: (Boolean) -> Unit) {}

fun test() {
    <!INAPPLICABLE_CANDIDATE!>bar<!>(::<!UNRESOLVED_REFERENCE!>foo<!>)
}
