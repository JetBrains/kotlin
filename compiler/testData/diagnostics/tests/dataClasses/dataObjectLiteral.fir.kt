// FIR_COMPARISON
// LANGUAGE: +DataObjects

interface I {
    fun foo()
}

val o = <!UNRESOLVED_REFERENCE!>data<!><!SYNTAX!><!> object<!SYNTAX!><!>: I {
    <!WRONG_MODIFIER_CONTAINING_DECLARATION!>override<!> fun foo() {}
}
