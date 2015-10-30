// KT-5508 Stackoverflow in type substitution

abstract class A<T> {
    public abstract fun Foo(x: T)

    public inner abstract class B<S> : A<B<S>>() {
        public inner class C<U> : B<C<U>>()
        {
            override fun Foo(x: B<C<U>>) {
                throw UnsupportedOperationException()
            }
        }
    }
}