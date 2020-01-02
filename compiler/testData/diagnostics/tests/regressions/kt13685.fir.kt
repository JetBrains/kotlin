// !WITH_NEW_INFERENCE
// !DIAGNOSTICS: -UNREACHABLE_CODE

fun foo() {
    val text: List<Any> = null!!
    text.<!UNRESOLVED_REFERENCE!>map<!> <!UNRESOLVED_REFERENCE!>Any<!><!SYNTAX!>?<!>::toString
}
