// FIR_IDENTICAL
// LANGUAGE: +DataObjects

interface I {
    fun foo()
}

val o = <!UNRESOLVED_REFERENCE!>data<!><!SYNTAX!><!> object<!SYNTAX!><!>: I {
    override fun foo() {}
}
