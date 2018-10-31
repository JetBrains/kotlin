<!NOTHING_TO_INLINE!>inline<!> fun foo() {
    <!UNRESOLVED_REFERENCE!>unresolved<!>().<!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>another<!>
    <!UNRESOLVED_REFERENCE!>unresolved<!>().<!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>another<!>()
}

fun main() {
    foo()
}
