// FIR_IDENTICAL
interface B<T> {
    fun f() = true
}

open class A(b: Boolean)

class C : B<Int> {
    inner class Inner : A(super<B>.f())
    inner class Inner2 : A(super<B<!TYPE_ARGUMENTS_REDUNDANT_IN_SUPER_QUALIFIER!><Int><!>>.f())

    fun test() {
        class LocalClass : A(super<B>.f())
        class LocalClass2 : A(super<B<!TYPE_ARGUMENTS_REDUNDANT_IN_SUPER_QUALIFIER!><Int><!>>.f())
    }
}
