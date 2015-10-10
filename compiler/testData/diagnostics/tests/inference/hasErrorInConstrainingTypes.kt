package n

fun <T> foo(<!UNUSED_PARAMETER!>t<!>: T, <!UNUSED_PARAMETER!>t1<!>: T) {}

fun test() {
    //no type inference error
    foo(<!UNRESOLVED_REFERENCE!>aaab<!>, <!UNRESOLVED_REFERENCE!>bbb<!>)
}