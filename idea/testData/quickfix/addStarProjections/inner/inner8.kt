// "Add star projections" "true"
class A<T, U> {
    inner class B<V, W> {
        inner class C<X, Y>
        fun test(x: Any) = x is C<caret>
    }
}