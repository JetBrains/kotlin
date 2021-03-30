package aaa

fun bar(a: Int, b: Int) {}

fun foo(a: Int?) {
    bar(a!!, <!DEBUG_INFO_SMARTCAST!>a<!>)
}
