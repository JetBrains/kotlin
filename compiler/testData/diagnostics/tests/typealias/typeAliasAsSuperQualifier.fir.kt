// !DIAGNOSTICS: -UNUSED_VARIABLE -UNUSED_PARAMETER -TOPLEVEL_TYPEALIASES_ONLY

open class Base {
    open fun foo() {}
}

open class GenericBase<T> {
    open fun foo() {}
}

class Unrelated {
    fun foo() {}
}

typealias B = Base
typealias U = Unrelated
typealias GB<T> = GenericBase<T>

class TestSuperForBase : B() {
    typealias MyBase = B

    override fun foo() {
        super<Base>.foo()
        super<B>.foo()
        super<<!UNRESOLVED_REFERENCE, UNRESOLVED_REFERENCE!>MyBase<!>>.<!UNRESOLVED_REFERENCE!>foo<!>()
        <!NOT_A_SUPERTYPE!>super<U><!>.foo()
    }
}

class TestSuperForGenericBase<T> : GB<T>() {
    typealias MyBase = GB<T>
    typealias MyBaseInt = GB<Int>

    override fun foo() {
        super<GenericBase>.foo()
        super<GB>.foo()
        super<<!UNRESOLVED_REFERENCE, UNRESOLVED_REFERENCE!>MyBase<!>>.<!UNRESOLVED_REFERENCE!>foo<!>()
        super<<!UNRESOLVED_REFERENCE, UNRESOLVED_REFERENCE!>MyBaseInt<!>>.<!UNRESOLVED_REFERENCE!>foo<!>() // Type arguments don't matter here
        <!NOT_A_SUPERTYPE!>super<U><!>.foo()
    }
}