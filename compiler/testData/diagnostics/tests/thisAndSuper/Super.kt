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
        super<<!NOT_A_SUPERTYPE!>E<!>>.<!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>bar<!>()
        super<<!NOT_A_SUPERTYPE!>E<!>>@A.<!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>bar<!>()
        super<<!NOT_A_SUPERTYPE!>Int<!>>.<!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>foo<!>()
        super<<!SYNTAX!><!>>.<!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>foo<!>()
        super<<!NOT_A_SUPERTYPE!>() -> Unit<!>>.<!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>foo<!>()
        super<<!NOT_A_SUPERTYPE!>Unit<!>>.<!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>foo<!>()
        <!DEBUG_INFO_MISSING_UNRESOLVED!>super<!><T><!UNRESOLVED_REFERENCE!>@B<!>.<!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>foo<!>()
        <!DEBUG_INFO_MISSING_UNRESOLVED!>super<!><C><!UNRESOLVED_REFERENCE!>@B<!>.<!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>bar<!>()
    }

    inner class B : T {
        fun test() {
            super<T>.foo();
            super<<!NOT_A_SUPERTYPE!>C<!>>.<!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>bar<!>()
            super<C>@A.bar()
            super<T>@A.foo()
            super<T>@B.foo()
            super<<!NOT_A_SUPERTYPE!>C<!>>@B.<!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>foo<!>()
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
        super<<!NOT_A_SUPERTYPE!>G<<!UNRESOLVED_REFERENCE!>E<!>><!>>.<!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>foo<!>() // Error
        super<<!NOT_A_SUPERTYPE!>G<String><!>>.<!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>foo<!>() // Error
    }
}

// The case when no supertype is resolved
class ERROR<E>() : <!UNRESOLVED_REFERENCE!>UR<!> {

    fun test() {
        super.<!UNRESOLVED_REFERENCE!>foo<!>()
    }
}
