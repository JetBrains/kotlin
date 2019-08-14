open class A

interface B<S, T : A>

typealias C<T> = B<T, A>

//        C<A> /* = B<A, A> */
//        │
class D : C<A>
