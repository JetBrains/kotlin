// FIR_IDENTICAL
package n

fun <T> foo(t: T, t1: T) {}

fun test() {
    //no type inference error
    foo(<!UNRESOLVED_REFERENCE!>aaab<!>, <!UNRESOLVED_REFERENCE!>bbb<!>)
}
