// "Create member function 'get'" "true"
// ERROR: operator modifier is required on 'get' in 'Foo'

class Foo<T, S: Iterable<T>> {
    fun <U> x (y: Foo<U, Iterable<U>>) {
        val z: U = y<caret>[""]
    }
}
