// "Create member function 'get'" "true"
// ERROR: operator modifier is required on 'get' in 'Foo'

class Foo<T> {
    fun <T, V> x (y: Foo<Iterable<T>>, w: Iterable<V>) {
        val z: Iterable<T> = y<caret>["", w]
    }
}
