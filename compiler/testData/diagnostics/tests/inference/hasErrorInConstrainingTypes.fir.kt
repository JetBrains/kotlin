package n

fun <T> foo(t: T, t1: T) {}

fun test() {
    //no type inference error
    <!CANNOT_INFER_PARAMETER_TYPE!>foo<!>(<!UNRESOLVED_REFERENCE!>aaab<!>, <!UNRESOLVED_REFERENCE!>bbb<!>)
}
