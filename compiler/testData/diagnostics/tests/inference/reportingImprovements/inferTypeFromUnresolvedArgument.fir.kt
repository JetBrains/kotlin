// !WITH_NEW_INFERENCE
// !DIAGNOSTICS: -UNUSED_PARAMETER

fun <K> id2(x: K, s: String): K = x
fun <K> ret(s: String): K = TODO()

fun test() {
    <!INAPPLICABLE_CANDIDATE!>id2<!>(<!UNRESOLVED_REFERENCE!>unresolved<!>, "foo")
    <!INAPPLICABLE_CANDIDATE!>id2<!>(<!UNRESOLVED_REFERENCE!>unresolved<!>, 42)

    ret("foo")
    <!INAPPLICABLE_CANDIDATE!>ret<!>(42)
}