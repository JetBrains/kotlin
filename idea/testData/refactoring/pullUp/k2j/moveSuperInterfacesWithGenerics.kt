// INFO: {"checked": "true"}
interface X<T, U>

// INFO: {"checked": "true"}
interface Y<V : Any>

// INFO: {"checked": "true"}
interface Z<T>

class <caret>B<S : Any, U : S, V>: A<S>(), X<U, V>, Y<S>, Z<U>