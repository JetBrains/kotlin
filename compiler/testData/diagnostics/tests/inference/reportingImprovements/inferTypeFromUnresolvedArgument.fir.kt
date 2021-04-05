// !WITH_NEW_INFERENCE
// !DIAGNOSTICS: -UNUSED_PARAMETER

fun <K> id2(x: K, s: String): K = x
fun <K> ret(s: String): K = TODO()

fun test() {
    id2(<!ARGUMENT_TYPE_MISMATCH, UNRESOLVED_REFERENCE!>unresolved<!>, "foo")
    id2(<!ARGUMENT_TYPE_MISMATCH, UNRESOLVED_REFERENCE!>unresolved<!>, <!ARGUMENT_TYPE_MISMATCH!>42<!>)

    ret("foo")
    ret(<!ARGUMENT_TYPE_MISMATCH!>42<!>)
}
