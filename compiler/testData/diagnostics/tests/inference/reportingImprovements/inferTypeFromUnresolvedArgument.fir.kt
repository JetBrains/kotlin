// !DIAGNOSTICS: -UNUSED_PARAMETER

fun <K> id2(x: K, s: String): K = x
fun <K> ret(s: String): K = TODO()

fun test() {
    id2(<!UNRESOLVED_REFERENCE!>unresolved<!>, "foo")
    id2(<!UNRESOLVED_REFERENCE!>unresolved<!>, <!ARGUMENT_TYPE_MISMATCH!>42<!>)

    <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>ret<!>("foo")
    ret(<!ARGUMENT_TYPE_MISMATCH!>42<!>)
}
