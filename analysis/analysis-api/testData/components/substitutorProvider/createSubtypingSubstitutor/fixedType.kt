interface <caret_subclass>A<T> : B<Int, T>
interface B<X, Y>

fun test(x: <caret_supertype>B<Int, String>) {}
