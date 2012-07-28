package example;

fun any(<!UNUSED_PARAMETER!>a<!> : Any) {}

fun notAnExpression() {
    any(<!SUPER_IS_NOT_AN_EXPRESSION!>super<!>) // not an expression
    if (<!SUPER_IS_NOT_AN_EXPRESSION!>super<!>) {} else {} // not an expression
    val <!UNUSED_VARIABLE!>x<!> = <!SUPER_IS_NOT_AN_EXPRESSION!>super<!> // not an expression
    <!NO_ELSE_IN_WHEN!>when<!> (1) {
        <!SUPER_IS_NOT_AN_EXPRESSION!>super<!> -> 1 // not an expression
    }

}

trait T {
    fun foo() {}
}
open class C() {
    fun bar() {}
}

class A<E>() : C(), T {

    fun test() {
        <!SUPER_IS_NOT_AN_EXPRESSION!>super<!>
        <!SUPER_IS_NOT_AN_EXPRESSION!>super<T><!>
        <!AMBIGUOUS_SUPER!>super<!>.foo()
        super<T>.foo()
        super<C>.bar()
        super<T>@A.foo()
        super<C>@A.bar()
        super<<!NOT_A_SUPERTYPE!>E<!>>.bar()
        super<<!NOT_A_SUPERTYPE!>E<!>>@A.bar()
        super<<!NOT_A_SUPERTYPE!>Int<!>>.foo()
        super<<!SYNTAX!><!>>.foo()
        super<<!NOT_A_SUPERTYPE!>() -> Unit<!>>.foo()
        super<<!NOT_A_SUPERTYPE!>#()<!>>.foo()
        super<T><!UNRESOLVED_REFERENCE!>@B<!>.foo()
        super<C><!UNRESOLVED_REFERENCE!>@B<!>.bar()
    }

    class B : T {
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

fun foo() {
    <!SUPER_IS_NOT_AN_EXPRESSION!>super<!>
    super.foo()
    super<Nothing>.foo()
}

trait G<T> {
    fun foo() {}
}

class CG : G<Int> {
    fun test() {
        super<G>.foo() // OK
        super<G<!TYPE_ARGUMENTS_REDUNDANT_IN_SUPER_QUALIFIER!><Int><!>>.foo() // Warning
        super<<!NOT_A_SUPERTYPE!>G<<!UNRESOLVED_REFERENCE!>E<!>><!>>.foo() // Error
        super<<!NOT_A_SUPERTYPE!>G<String><!>>.foo() // Error
    }
}

// The case when no supertype is resolved
class ERROR<E>() : <!UNRESOLVED_REFERENCE!>UR<!> {

    fun test() {
        super.<!UNRESOLVED_REFERENCE!>foo<!>()
    }
}

// No supertype at all
class A1 {
    fun test() {
        <!SUPER_IS_NOT_AN_EXPRESSION!>super<!>.equals(null)
    }
}
