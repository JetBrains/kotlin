// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
// RENDER_DIAGNOSTICS_FULL_TEXT
interface I {
    fun foo(s: String)
    fun bar(a: String)
    fun bar(a: Boolean)
    fun baz(a: Int = 1)
    fun qux(vararg a: String)
    fun quux(a: (s: String)->Unit)
    fun String.corge()
}

<!ABSTRACT_MEMBER_NOT_IMPLEMENTED!>class Simple<!> : I {
    <!NOTHING_TO_OVERRIDE!>override<!> fun foo() {}
}

class SameClass {
    <!NOTHING_TO_OVERRIDE!>override<!> fun foo() {}
    fun foo(s: String) {}
}

<!ABSTRACT_MEMBER_NOT_IMPLEMENTED!>class OverloadedMethods<!> : I {
    <!NOTHING_TO_OVERRIDE!>override<!> fun bar(a: Int) {}
}

<!ABSTRACT_MEMBER_NOT_IMPLEMENTED!>class DefaultParameters<!> : I {
    <!NOTHING_TO_OVERRIDE!>override<!> fun baz(a: String) {}
}

<!ABSTRACT_MEMBER_NOT_IMPLEMENTED!>class VarargParameters<!> : I {
    <!NOTHING_TO_OVERRIDE!>override<!> fun qux(a: String){}
}

<!ABSTRACT_MEMBER_NOT_IMPLEMENTED!>class FunctionalType<!> : I {
    <!NOTHING_TO_OVERRIDE!>override<!> fun quux(a: ()->Any){}
}

<!ABSTRACT_MEMBER_NOT_IMPLEMENTED!>class ExtensionFunction<!>: I {
    <!NOTHING_TO_OVERRIDE!>override<!> fun Any.corge(){}
}

interface Generic<T> {
    fun foo(t: T)
}

<!ABSTRACT_MEMBER_NOT_IMPLEMENTED!>class GenericOverride<!><R> : Generic<R> {
    <!NOTHING_TO_OVERRIDE!>override<!> fun foo() {}
}

<!ABSTRACT_MEMBER_NOT_IMPLEMENTED!>class NonGenericOverride<!> : Generic<String> {
    <!NOTHING_TO_OVERRIDE!>override<!> fun foo() {}
}

open class HasFinal {
    fun foo(s: String) {}
}

class ExtendsHasFinal {
    <!NOTHING_TO_OVERRIDE!>override<!> fun foo() {}
}