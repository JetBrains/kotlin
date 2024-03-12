// LL_FIR_DIVERGENCE
// WRONG_NUMBER_OF_TYPE_ARGUMENTS not reported in Inner class because of BodyBuildingMode.LAZY_BODIES
// LL_FIR_DIVERGENCE
interface B<T> {
    fun f() = true
}

open class A(b: Boolean)

class C : B<Int> {
    inner class Inner : A(super<B>.f())
    inner class Inner2 : A(super<B<!TYPE_ARGUMENTS_REDUNDANT_IN_SUPER_QUALIFIER!><Int><!>>.f())

    fun test() {
        class LocalClass : A(super<<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>B<!>>.<!UNRESOLVED_REFERENCE!>f<!>())
        class LocalClass2 : A(super<B<!TYPE_ARGUMENTS_REDUNDANT_IN_SUPER_QUALIFIER!><Int><!>>.f())
    }
}
