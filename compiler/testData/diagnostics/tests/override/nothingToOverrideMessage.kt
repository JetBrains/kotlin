// FIR_IDENTICAL
// RENDER_DIAGNOSTICS_FULL_TEXT
interface I {
    fun foo(s: String)
}

<!ABSTRACT_MEMBER_NOT_IMPLEMENTED!>class Simple<!> : I {
    <!NOTHING_TO_OVERRIDE!>override<!> fun foo() {}
}

class SameClass {
    <!NOTHING_TO_OVERRIDE!>override<!> fun foo() {}
    fun foo(s: String) {}
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