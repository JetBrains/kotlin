// !DIAGNOSTICS: -UNREACHABLE_CODE

fun foo() {
    val <!UNUSED_VARIABLE!>text<!>: List<Any> = null!!
    text.<!UNRESOLVED_REFERENCE!>map<!> <!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>Any<!><!SYNTAX!>?<!>::<!OVERLOAD_RESOLUTION_AMBIGUITY!>toString<!>
}
