package n

fun <T> foo(t: T, t1: T) {}

fun test() {
    //no type inference error
    foo(<!ARGUMENT_TYPE_MISMATCH, UNRESOLVED_REFERENCE!>aaab<!>, <!ARGUMENT_TYPE_MISMATCH, UNRESOLVED_REFERENCE!>bbb<!>)
}