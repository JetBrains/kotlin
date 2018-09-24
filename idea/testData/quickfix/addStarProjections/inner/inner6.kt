// "Add star projections" "true"
class A {
    class B<T> {
        inner class C<U> {
            inner class D<V>
        }
    }
    fun test(x: Any) = x is B.C<caret>.D
}