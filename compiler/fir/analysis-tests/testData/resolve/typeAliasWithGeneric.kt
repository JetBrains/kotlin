open class A

interface B<S, T : A>

class D : C<A>

typealias C<T> = B<T, A>
