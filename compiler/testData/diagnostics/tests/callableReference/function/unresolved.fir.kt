// !DIAGNOSTICS: -UNUSED_EXPRESSION, -UNUSED_PARAMETER
// !WITH_NEW_INFERENCE

class A

fun test1() {
    val foo = ::foo

    ::bar

    A::bar

    <!UNRESOLVED_REFERENCE!>B<!>::bar
}

fun test2() {
    fun foo(x: Any) {}
    fun foo() {}

    <!UNRESOLVED_REFERENCE!>Unresolved<!>::foo
    <!INAPPLICABLE_CANDIDATE!>foo<!>(<!UNRESOLVED_REFERENCE!>Unresolved<!>::foo)
    <!INAPPLICABLE_CANDIDATE!>foo<!>(<!UNRESOLVED_REFERENCE!>Unresolved<!>::unresolved)
    ::unresolved
}