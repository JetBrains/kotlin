// "Add star projections" "true"
class A {
    class B<T> {
        class C<U> {
            inner class D
        }
    }
    fun test(x: Any) = x is B.C.D<caret>
}