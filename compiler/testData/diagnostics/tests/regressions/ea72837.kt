// !DIAGNOSTICS: -UNUSED_PARAMETER
fun <T> g(x: T) = 1
fun h(x: () -> Unit) = 1

fun foo() {
    <!UNRESOLVED_REFERENCE!>f<!>(::<!SYNTAX!><!>)
    g(::<!SYNTAX!><!>)
    h(::<!SYNTAX!><!>)
}
