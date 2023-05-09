// !LANGUAGE: -ProhibitVisibilityOfNestedClassifiersFromSupertypesOfCompanion

// FILE: 1.kt
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

// FILE: C.java
public class C extends B implements A {

}

// FILE: 2.kt
class D: C() {
    init {
        <!UNRESOLVED_REFERENCE!>foo<!>()
        A.foo()
        A.Companion.foo()
        C.<!UNRESOLVED_REFERENCE!>foo<!>()
        D.<!UNRESOLVED_REFERENCE!>foo<!>()

        <!UNRESOLVED_REFERENCE!>A_<!>()
        A.<!UNRESOLVED_REFERENCE!>A_<!>()
        A.Companion.A_()
        C.<!UNRESOLVED_REFERENCE!>A_<!>()
        D.<!UNRESOLVED_REFERENCE!>A_<!>()

        bar()
        B.bar()
        B.Companion.bar()
        C.<!UNRESOLVED_REFERENCE!>bar<!>()
        D.<!UNRESOLVED_REFERENCE!>bar<!>()

        <!DEPRECATED_ACCESS_BY_SHORT_NAME!>B_()<!>
        B.<!UNRESOLVED_REFERENCE!>B_<!>()
        B.Companion.B_()
        C.<!UNRESOLVED_REFERENCE!>B_<!>()
        D.<!UNRESOLVED_REFERENCE!>B_<!>()
    }
}
