//!DIAGNOSTICS: -UNUSED_PARAMETER

fun foo<T>(i: Int, t: T) {}
fun foo<T>(s: String, t: T) {}

fun bar(i: Int) {}
fun bar(s: String) {}

fun test() {
    foo(<!UNRESOLVED_REFERENCE!>rrr<!>, 1)
    bar(<!UNRESOLVED_REFERENCE!>rrr<!>)
}
