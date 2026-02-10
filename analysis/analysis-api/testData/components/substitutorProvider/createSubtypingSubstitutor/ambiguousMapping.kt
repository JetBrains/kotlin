// When the same type parameter would map to different types - should return null
interface <caret_subclass>A<T> : B<T, T>
interface B<X, Y>

// T would need to be both Int and String - impossible
fun test(x: <caret_supertype>B<Int, String>) {}
