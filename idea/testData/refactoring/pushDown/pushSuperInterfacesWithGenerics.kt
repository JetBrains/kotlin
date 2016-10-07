interface I

// INFO: {"checked": "true"}
interface X<T>

// INFO: {"checked": "true"}
interface Y<T>

// INFO: {"checked": "true"}
interface Z<T>

open class <caret>A<T, U> : X<T>, Y<X<I>>, Z<Y<U>>

class B<S> : A<X<S>, Y<S>>(), Z<I>