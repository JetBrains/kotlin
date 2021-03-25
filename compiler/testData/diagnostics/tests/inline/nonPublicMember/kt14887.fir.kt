inline fun foo() {
    <!UNRESOLVED_REFERENCE!>unresolved<!>().another
    <!UNRESOLVED_REFERENCE!>unresolved<!>().another()
}

fun main() {
    foo()
}
