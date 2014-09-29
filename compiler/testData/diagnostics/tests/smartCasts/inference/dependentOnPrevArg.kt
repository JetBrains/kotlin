package a

fun <T> foo(u: T, <!UNUSED_PARAMETER!>v<!>: T): T = u

fun test(s: String?) {
    val <!UNUSED_VARIABLE!>r<!>: String = foo(s!!, <!DEBUG_INFO_SMARTCAST!>s<!>)
}