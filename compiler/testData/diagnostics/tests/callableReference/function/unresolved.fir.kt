// !DIAGNOSTICS: -UNUSED_EXPRESSION, -UNUSED_PARAMETER
// !WITH_NEW_INFERENCE

class A

fun test1() {
    val foo = <!UNRESOLVED_REFERENCE!>::foo<!>

    <!UNRESOLVED_REFERENCE!>::bar<!>

    <!UNRESOLVED_REFERENCE!>A::bar<!>

    <!UNRESOLVED_REFERENCE!><!UNRESOLVED_REFERENCE!>B<!>::bar<!>
}

fun test2() {
    fun foo(x: Any) {}
    fun foo() {}

    <!UNRESOLVED_REFERENCE!><!UNRESOLVED_REFERENCE!>Unresolved<!>::foo<!>
    <!INAPPLICABLE_CANDIDATE!>foo<!>(<!UNRESOLVED_REFERENCE!><!UNRESOLVED_REFERENCE!>Unresolved<!>::foo<!>)
    <!INAPPLICABLE_CANDIDATE!>foo<!>(<!UNRESOLVED_REFERENCE!><!UNRESOLVED_REFERENCE!>Unresolved<!>::unresolved<!>)
    <!UNRESOLVED_REFERENCE!>::unresolved<!>
}