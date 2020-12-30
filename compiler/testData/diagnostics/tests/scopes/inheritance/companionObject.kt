// !LANGUAGE: -ProhibitVisibilityOfNestedClassifiersFromSupertypesOfCompanion

interface A {
    companion object {
        fun foo() {}

        class A_
    }
}

open class B {
    companion object {
        fun bar() {}

        class B_
    }
}

class C: B(), A {
    init {
        <!UNRESOLVED_REFERENCE!>foo<!>()
        A.foo()
        A.Companion.foo()
        C.<!UNRESOLVED_REFERENCE!>foo<!>()

        <!UNRESOLVED_REFERENCE!>A_<!>()
        A.<!UNRESOLVED_REFERENCE!>A_<!>()
        A.Companion.A_()
        C.<!UNRESOLVED_REFERENCE!>A_<!>()

        bar()
        B.bar()
        B.Companion.bar()
        C.<!UNRESOLVED_REFERENCE!>bar<!>()

        <!DEPRECATED_ACCESS_BY_SHORT_NAME!>B_()<!>
        B.<!UNRESOLVED_REFERENCE!>B_<!>()
        B.Companion.B_()
        C.<!UNRESOLVED_REFERENCE!>B_<!>()
    }
}
