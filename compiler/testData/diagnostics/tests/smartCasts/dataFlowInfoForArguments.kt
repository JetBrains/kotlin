package aaa

fun bar(<!UNUSED_PARAMETER!>a<!>: Int, <!UNUSED_PARAMETER!>b<!>: Int) {}

fun foo(a: Int?) {
    bar(a!!, <!DEBUG_INFO_SMARTCAST!>a<!>)
}
