package n

fun <T> foo(t: T, t1: T) {}

fun test() {
    //no type inference error
    <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>foo<!>(<!UNRESOLVED_REFERENCE!>aaab<!>, <!UNRESOLVED_REFERENCE!>bbb<!>)
}
