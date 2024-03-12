interface B<T> {
    fun f() = true
}

open class A(b: Boolean)

class C : B<Int> {
    inner class Inner : A(super<<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>B<!>>.<!UNRESOLVED_REFERENCE!>f<!>())
    inner class Inner2 : A(super<B<!TYPE_ARGUMENTS_REDUNDANT_IN_SUPER_QUALIFIER!><Int><!>>.f())

    fun test() {
        class LocalClass : A(super<<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>B<!>>.<!UNRESOLVED_REFERENCE!>f<!>())
        class LocalClass2 : A(super<B<!TYPE_ARGUMENTS_REDUNDANT_IN_SUPER_QUALIFIER!><Int><!>>.f())
    }
}
