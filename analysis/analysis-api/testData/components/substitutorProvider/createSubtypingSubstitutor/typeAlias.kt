interface <caret_subclass>A<T> : B<T>
interface B<T> : C<Int, T>
interface C<X, Y>

typealias CAlias<P, Q> = C<P, Q>

fun test(x: <caret_supertype>CAlias<Int, String>) {}
