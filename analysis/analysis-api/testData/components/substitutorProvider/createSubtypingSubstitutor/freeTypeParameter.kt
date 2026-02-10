// U will remain unsubstituted
interface <caret_subclass>A<T, U> : B<T>
interface B<X>

fun test(x: <caret_supertype>B<Int>) {}
