interface <caret_subclass>A<T> : B<T>
interface B<T> : C<Int, T>
interface C<X, Y>

fun test(x: <caret_supertype>C<Int, String>) {}
