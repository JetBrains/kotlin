// Star projections in supertype should return null (cannot determine concrete type)
interface <caret_subclass>A<T> : B<T>
interface B<T>

fun test(x: <caret_supertype>B<*>) {}
