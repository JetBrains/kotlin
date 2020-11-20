package example

interface T {
    fun foo() {}
}
open class C() {
    fun bar() {}
}

class A<E>() : C(), T {

    fun test() {
        super
        super<T>
        super.foo()
        super<T>.foo()
        super<C>.bar()
        super<T>@A.foo()
        super<C>@A.bar()
        super<<!OTHER_ERROR, OTHER_ERROR!>E<!>>.<!UNRESOLVED_REFERENCE!>bar<!>()
        super<<!OTHER_ERROR, OTHER_ERROR!>E<!>>@A.<!UNRESOLVED_REFERENCE!>bar<!>()
        <!NOT_A_SUPERTYPE!>super<Int><!>.<!UNRESOLVED_REFERENCE!>foo<!>()
        super<<!SYNTAX!><!>>.<!UNRESOLVED_REFERENCE!>foo<!>()
        <!NOT_A_SUPERTYPE!>super<() -> Unit><!>.<!UNRESOLVED_REFERENCE!>foo<!>()
        <!NOT_A_SUPERTYPE!>super<Unit><!>.<!UNRESOLVED_REFERENCE!>foo<!>()
        super<T>@B.foo()
        super<C>@B.bar()
    }

    inner class B : T {
        fun test() {
            super<T>.foo();
            <!NOT_A_SUPERTYPE!>super<C><!>.bar()
            super<C>@A.bar()
            super<T>@A.foo()
            super<T>@B.foo()
            <!NOT_A_SUPERTYPE!>super<C>@B<!>.<!UNRESOLVED_REFERENCE!>foo<!>()
            super.foo()
            super
            super<T>
        }
    }
}

interface G<T> {
    fun foo() {}
}

class CG : G<Int> {
    fun test() {
        super<G>.foo() // OK
        super<G<Int>>.foo() // Warning
        super<G<E>>.<!UNRESOLVED_REFERENCE!>foo<!>() // Error
        super<G<String>>.foo() // Error
    }
}

// The case when no supertype is resolved
class ERROR<E>() : <!UNRESOLVED_REFERENCE!>UR<!> {

    fun test() {
        super.<!UNRESOLVED_REFERENCE!>foo<!>()
    }
}
