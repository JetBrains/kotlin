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
        super.<!UNRESOLVED_REFERENCE!>foo<!>()
        super<T>.foo()
        super<C>.bar()
        super<T>@A.foo()
        super<C>@A.bar()
        super<E>.<!UNRESOLVED_REFERENCE!>bar<!>()
        super<E>@A.<!UNRESOLVED_REFERENCE!>bar<!>()
        super<Int>.<!UNRESOLVED_REFERENCE!>foo<!>()
        super<<!SYNTAX!><!>>.<!UNRESOLVED_REFERENCE!>foo<!>()
        super<() -> Unit>.<!UNRESOLVED_REFERENCE!>foo<!>()
        super<Unit>.<!UNRESOLVED_REFERENCE!>foo<!>()
        super<T>@B.foo()
        super<C>@B.bar()
    }

    inner class B : T {
        fun test() {
            super<T>.foo();
            super<C>.bar()
            super<C>@A.bar()
            super<T>@A.foo()
            super<T>@B.foo()
            super<C>@B.<!UNRESOLVED_REFERENCE!>foo<!>()
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
        super<G<E>>.foo() // Error
        super<G<String>>.foo() // Error
    }
}

// The case when no supertype is resolved
class ERROR<E>() : UR {

    fun test() {
        super.<!UNRESOLVED_REFERENCE!>foo<!>()
    }
}
