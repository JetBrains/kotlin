interface <caret_subclass>A<T> : B<Int, T>
interface B<X, Y>

// X is Int in A's inheritance, but String is provided here
fun test(x: <caret_supertype>B<String, String>) {}
