open class A: Z<String> {

}

// INFO: {"checked": "true"}
interface X<T, U>

// INFO: {"checked": "true"}
interface Y<V : Any>

// INFO: {"checked": "true"}
interface Z<T>

class <caret>B<T : Any, U, V>: A(), X<U, V>, Y<T>, Z<U>