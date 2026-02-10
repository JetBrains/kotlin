// WITH_STDLIB

interface <caret_subclass>A<T, S> : B<S, T>
interface B<X, Y> : C<Y, X, List<X>>
interface C<P, Q, R>

fun test(x: <caret_supertype>C<String, Int, List<Int>>) {}
