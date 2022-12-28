// FIR_IDENTICAL
// KT-5508 Stackoverflow in type substitution

abstract class A<T> {
    public abstract fun foo(x: T)
    public abstract fun bar(x: T)

    public inner abstract class B<S> : A<B<S>>() {
        public inner <!ABSTRACT_CLASS_MEMBER_NOT_IMPLEMENTED!>class C<!><U> : B<C<U>>()
        {
            // Here B<C<U>> means A<A<A<T>.B<S>>.B<A<T>.B<S>.C<U>>>.B<A<A<T>.B<S>>.B<A<T>.B<S>.C<U>>.C<U>>
            // while for being a correct override it should be A<A<T>.B<S>>.B<A<T>.B<S>.C<U>>
            // It happens because at the beginning we search implicit arguments for an outer classes through supertypes
            // See TypeResolver.computeImplicitOuterClassArguments for clarifications
            <!NOTHING_TO_OVERRIDE!>override<!> fun foo(x: B<C<U>>)  {
                throw UnsupportedOperationException()
            }

            override fun bar(x: A<A<T>.B<S>>.B<A<T>.B<S>.C<U>>) {
                throw UnsupportedOperationException()
            }
        }
    }
}
