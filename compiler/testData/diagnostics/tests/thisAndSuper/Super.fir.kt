package example

interface T {
    fun foo() {}
}
open class C() {
    fun bar() {}
}

class A<E>() : C(), T {

    fun test() {
        <!SUPER_IS_NOT_AN_EXPRESSION!>super<!>
        <!SUPER_IS_NOT_AN_EXPRESSION!>super<T><!>
        super.foo()
        super<T>.foo()
        super<C>.bar()
        super<T>@A.foo()
        super<C>@A.bar()
        super<<!NOT_A_SUPERTYPE!>E<!>>.bar()
        super<<!NOT_A_SUPERTYPE!>E<!>>@A.bar()
        super<<!NOT_A_SUPERTYPE!>Int<!>>.foo()
        super<<!SYNTAX!><!>>.<!UNRESOLVED_REFERENCE!>foo<!>()
        super<<!NOT_A_SUPERTYPE!>() -> Unit<!>>.foo()
        super<<!NOT_A_SUPERTYPE!>Unit<!>>.foo()
        super<T><!UNRESOLVED_LABEL!>@B<!>.foo()
        super<C><!UNRESOLVED_LABEL!>@B<!>.bar()
    }

    inner class B : T {
        fun test() {
            super<T>.foo();
            super<<!NOT_A_SUPERTYPE!>C<!>>.bar()
            super<C>@A.bar()
            super<T>@A.foo()
            super<T>@B.foo()
            super<<!NOT_A_SUPERTYPE!>C<!>>@B.foo()
            super.foo()
            <!SUPER_IS_NOT_AN_EXPRESSION!>super<!>
            <!SUPER_IS_NOT_AN_EXPRESSION!>super<T><!>
        }
    }
}

interface G<T> {
    fun foo() {}
}

class CG : G<Int> {
    fun test() {
        super<G>.foo() // OK
        super<G<!TYPE_ARGUMENTS_REDUNDANT_IN_SUPER_QUALIFIER!><Int><!>>.foo() // Warning
        super<<!NOT_A_SUPERTYPE!>G<E><!>>.foo() // Error
        super<<!NOT_A_SUPERTYPE!>G<String><!>>.foo() // Error
    }
}

// The case when no supertype is resolved
class ERROR<E>() : <!UNRESOLVED_REFERENCE!>UR<!> {

    fun test() {
        <!UNRESOLVED_REFERENCE!>super<!>.<!UNRESOLVED_REFERENCE!>foo<!>()
    }
}
