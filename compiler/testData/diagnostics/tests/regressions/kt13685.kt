// DIAGNOSTICS: -UNREACHABLE_CODE

fun foo() {
    val text: List<Any> = null!!
    text.<!UNRESOLVED_REFERENCE!>map<!> <!DEBUG_INFO_MISSING_UNRESOLVED!>Any<!><!SYNTAX!>?<!>::<!DEBUG_INFO_MISSING_UNRESOLVED!>toString<!>
}
