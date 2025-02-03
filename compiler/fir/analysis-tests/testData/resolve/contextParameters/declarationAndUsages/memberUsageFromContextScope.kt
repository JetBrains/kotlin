// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ContextParameters

class A

class Base {
    context(a: A)
    fun funMember() { }
}

context(a: A, b: Base)
fun usageFromOnlyContextScope() {
    a.<!UNRESOLVED_REFERENCE!>funMember<!>()
    b.funMember()
    Base().funMember()
}