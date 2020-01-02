inline fun foo() {
    <!UNRESOLVED_REFERENCE!>unresolved<!>().<!UNRESOLVED_REFERENCE!>another<!>
    <!UNRESOLVED_REFERENCE!>unresolved<!>().<!UNRESOLVED_REFERENCE!>another<!>()
}

fun main() {
    foo()
}
