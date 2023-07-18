// !DIAGNOSTICS: -UNUSED_EXPRESSION, -UNUSED_PARAMETER

class A

fun test1() {
    val foo = ::<!UNRESOLVED_REFERENCE!>foo<!>

    ::<!UNRESOLVED_REFERENCE!>bar<!>

    A::<!UNRESOLVED_REFERENCE!>bar<!>

    <!UNRESOLVED_REFERENCE!>B<!>::bar
}

fun test2() {
    fun foo(x: Any) {}
    fun foo() {}

    <!UNRESOLVED_REFERENCE!>Unresolved<!>::foo
    foo(<!UNRESOLVED_REFERENCE!>Unresolved<!>::foo)
    foo(<!UNRESOLVED_REFERENCE!>Unresolved<!>::unresolved)
    ::<!UNRESOLVED_REFERENCE!>unresolved<!>
}
