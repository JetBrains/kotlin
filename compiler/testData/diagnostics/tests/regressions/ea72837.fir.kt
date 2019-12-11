// !WITH_NEW_INFERENCE
// !DIAGNOSTICS: -UNUSED_PARAMETER
fun <T> g(x: T) = 1
fun h(x: () -> Unit) = 1

fun foo() {
    <!UNRESOLVED_REFERENCE!>f<!>(::<!SYNTAX!><!>)
    <!INAPPLICABLE_CANDIDATE!>g<!>(::<!SYNTAX!><!>)
    <!INAPPLICABLE_CANDIDATE!>h<!>(::<!SYNTAX!><!>)
}
