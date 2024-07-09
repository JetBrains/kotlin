// FIR_IDENTICAL
// WITH_STDLIB
// DIAGNOSTICS: -UNUSED_PARAMETER, -DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE, -DEBUG_INFO_MISSING_UNRESOLVED

fun <T> foo(i: Int, t: T) {}
fun <T> foo(s: String, t: T) {}

fun bar(i: Int) {}
fun bar(s: String) {}

fun test() {
    foo(<!UNRESOLVED_REFERENCE!>rrr<!>, 1)
    bar(<!UNRESOLVED_REFERENCE!>rrr<!>)
}

fun bar(x: <!UNRESOLVED_REFERENCE!>Unresolved<!>) = x + 1
