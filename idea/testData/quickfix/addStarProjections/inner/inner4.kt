// "Add star projections" "true"
class A {
    class B<T> {
        inner class C<U> {
            inner class D
            fun test(x: Any) = x is D<caret>
        }
    }
}