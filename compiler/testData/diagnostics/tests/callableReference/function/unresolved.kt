// DIAGNOSTICS: -UNUSED_EXPRESSION, -UNUSED_PARAMETER

class A

fun test1() {
    val foo = ::<!UNRESOLVED_REFERENCE!>foo<!>

    ::<!UNRESOLVED_REFERENCE!>bar<!>

    A::<!UNRESOLVED_REFERENCE!>bar<!>

    <!UNRESOLVED_REFERENCE!>B<!>::<!DEBUG_INFO_MISSING_UNRESOLVED!>bar<!>
}

fun test2() {
    fun foo(x: Any) {}
    fun foo() {}

    <!UNRESOLVED_REFERENCE!>Unresolved<!>::<!DEBUG_INFO_MISSING_UNRESOLVED!>foo<!>
    foo(<!UNRESOLVED_REFERENCE!>Unresolved<!>::<!CALLABLE_REFERENCE_RESOLUTION_AMBIGUITY!>foo<!>)
    foo(<!UNRESOLVED_REFERENCE!>Unresolved<!>::<!UNRESOLVED_REFERENCE!>unresolved<!>)
    ::<!UNRESOLVED_REFERENCE!>unresolved<!>
}
