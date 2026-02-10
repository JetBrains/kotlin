// When the same type parameter maps to the same type multiple times - should work
interface <caret_subclass>A<T> : B<T, T>
interface B<X, Y>

// T maps to Int from both X and Y - consistent
fun test(x: <caret_supertype>B<Int, Int>) {}
