<!NOTHING_TO_INLINE!>inline<!> fun foo() {
    <!UNRESOLVED_REFERENCE!>unresolved<!>().<!DEBUG_INFO_MISSING_UNRESOLVED!>another<!>
    <!UNRESOLVED_REFERENCE!>unresolved<!>().<!DEBUG_INFO_MISSING_UNRESOLVED!>another<!>()
}

fun main() {
    foo()
}
