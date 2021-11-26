// FIR_IDENTICAL
class A<T: B<out Number>>(val x: T) {
    fun test() {
        val y: Int = x.m<<!UPPER_BOUND_VIOLATED!>C<out Number><!>>()
    }

}

class B<T1> {
    fun <X1: C<T1>> m(): Int = 1
}

class C<T>
